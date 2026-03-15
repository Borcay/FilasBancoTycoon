import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class SimulacionBanco {

    private static final int META_CLIENTES  = 200;
    private static final long DURACION_DIA  = 60_000;
    private static final Random random      = new Random();

    // ── Ladrones activos visibles para la GUI ──
    private final CopyOnWriteArrayList<Ladron> ladronesActivos = new CopyOnWriteArrayList<>();
    private final AtomicBoolean primerLadronAvisado = new AtomicBoolean(false);

    // ── Meta diaria ──
    private volatile boolean bonusMetaOtorgadoHoy = false;

    // ── Estadísticas de ladrones por día ──
    private static final long RECOMPENSA_ATRAPA = 20L;
    private volatile int ladronesAtrapados = 0;
    private volatile int ladronesRobaron   = 0;

    private static final String[] NOMBRES_CAJERO =
        {"Pata","Patito","Mano","Manita","Beto","Caro","Tito","Lina"};

    private final List<Cajero> cajeros = new ArrayList<>();
    private final AtomicInteger totalAtendidos = new AtomicInteger(0);
    private final AtomicInteger hoyAtendidos   = new AtomicInteger(0);

    private volatile boolean terminado = false;
    private volatile boolean prestigioPendiente = false; // 200 alcanzados, esperando decisión
    private final Economia eco;
    private InterfazGrafica gui;
    private final SonidoManager sonido;
    private PrestigioManager prestige; // puede ser null si no hay prestige aún

    private volatile Thread hiloGenerador;
    private PaseBatalla paseBatalla;

    public SimulacionBanco(Economia eco) {
        this.eco    = eco;
        this.sonido = new SonidoManager();
        this.paseBatalla = new PaseBatalla(eco);
        eco.setPaseBatalla(paseBatalla);
        inicializarCajeros(eco.getMaxCajeros());
    }

    /** Asociar el PrestigioManager global (llamado desde Main después de construir) */
    public void setPrestigio(PrestigioManager p) {
        this.prestige = p;
        eco.setPrestigio(p);
        // Si hay mejoras iniciales desbloqueadas, aplicarlas
        if (p != null && p.tieneMejorasIniciales()) {
            eco.mejorarVelocidadGratis();
            eco.mejorarFlujoGratis();
        }
    }

    private void inicializarCajeros(int n) {
        int rapidoIdx = random.nextInt(n);
        for (int i = 0; i < n; i++) {
            cajeros.add(new Cajero(
                NOMBRES_CAJERO[i % NOMBRES_CAJERO.length],
                i == rapidoIdx, this, eco));
        }
    }

    public void setGui(InterfazGrafica g) {
        this.gui = g;
        if (paseBatalla != null && g != null) {
            paseBatalla.setOnSubirNivel(() ->
                javax.swing.SwingUtilities.invokeLater(() -> g.notificarSubidaNivel(paseBatalla.getUltimoNivelSubido()))
            );
        }
    }

    public void iniciar() {
        sonido.iniciarMusicaFondo();
        cajeros.forEach(Cajero::iniciar);
        iniciarGeneradorClientes();
        iniciarGeneradorLadrones();
        iniciarBalanceador();
        iniciarCicloDia();
    }

    private void iniciarGeneradorClientes() {
        hiloGenerador = new Thread(() -> {
            while (!terminado) {
                try {
                    Thread.sleep(eco.getSpawnIntervaloMs());
                    if (terminado) break;
                    List<Cajero> abiertos = getCajerosAbiertos();
                    if (abiertos.isEmpty()) continue;
                    boolean vip = random.nextDouble() < eco.getProbVip();
                    Cliente cl  = new Cliente(vip);
                    Cajero dst  = abiertos.stream()
                        .min(Comparator.comparingInt(Cajero::getTamanioCola)).orElse(abiertos.get(0));
                    dst.agregarCliente(cl);

                    // Verificar meta diaria
                    verificarMetaDiaria();

                    if (gui != null) gui.repaint();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); break;
                }
            }
        }, "Generador");
        hiloGenerador.setDaemon(true);
        hiloGenerador.start();
    }

    /** Genera ladrones cada 20-40 segundos (modificado por habilidad de prestige) */
    private void iniciarGeneradorLadrones() {
        Thread t = new Thread(() -> {
            try { Thread.sleep(15000 + random.nextInt(10000)); }
            catch (InterruptedException e) { return; }

            while (!terminado) {
                try {
                    if (terminado) break;
                    List<Cajero> abiertos = getCajerosAbiertos();
                    if (!abiertos.isEmpty()) {
                        Ladron ladron = new Ladron();
                        Cajero dst = abiertos.get(random.nextInt(abiertos.size()));
                        dst.agregarCliente(ladron);
                        ladronesActivos.add(ladron);

                        if (primerLadronAvisado.compareAndSet(false, true) && gui != null) {
                            javax.swing.SwingUtilities.invokeLater(() ->
                                gui.mostrarAvisoLadron());
                        }
                    }
                    // Aplicar multiplicador de prestige al intervalo
                    double mult = (prestige != null) ? prestige.multiplicadorIntervaloLadrones() : 1.0;
                    long baseMs = (long)((20000 + random.nextInt(20000)) * mult);
                    Thread.sleep(baseMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); break;
                }
            }
        }, "GeneradorLadrones");
        t.setDaemon(true);
        t.start();
    }

    /** El jugador hizo clic en un ladrón: lo atrapa */
    public boolean intentarAtraparLadron(int mouseX, int mouseY) {
        for (Ladron l : ladronesActivos) {
            if (l.isEliminado() || l.isAtrapado()) continue;
            double dx = mouseX - (l.getX() + 36);
            double dy = mouseY - (l.getY() + 23);
            if (dx*dx + dy*dy < 40*40) {
                l.setAtrapado(true);
                l.setEliminado(true);
                l.setMostrandoPolicia(true);
                l.setAlphaPolicia(1.0f);
                for (Cajero c : cajeros) c.getCola().remove(l);
                ladronesAtrapados++;
                // Recompensa por atrapar al ladrón
                eco.agregarMonedas(RECOMPENSA_ATRAPA);
                if (paseBatalla != null) paseBatalla.agregarXP(PaseBatalla.XP_LADRON_ATRAPADO);
                if (gui != null) {
                    final int fx = (int)l.getX(), fy = (int)l.getY();
                    javax.swing.SwingUtilities.invokeLater(() ->
                        gui.mostrarRecompensaLadron(fx, fy, RECOMPENSA_ATRAPA));
                }
                new Thread(() -> {
                    try { Thread.sleep(1200); }
                    catch (InterruptedException ignored) {}
                    ladronesActivos.remove(l);
                }).start();
                return true;
            }
        }
        return false;
    }

    /** Llamado por Cajero cuando atiende a un cliente — si es ladrón, roba */
    public void ladronRobo(Ladron l) {
        if (l.isAtrapado() || l.isEliminado()) return;
        l.setRobando(true);
        l.setEliminado(true);
        ladronesActivos.remove(l);
        ladronesRobaron++;
        long robo = 50;
        // No bajar de 0
        // usamos Economia directamente para descontar
        eco.descontarMonedas(robo);
        if (gui != null) {
            javax.swing.SwingUtilities.invokeLater(() ->
                gui.mostrarRoboLadron((int)l.getX(), (int)l.getY(), robo));
        }
    }

    /** Verifica si se alcanzó la meta diaria y otorga bonus */
    private void verificarMetaDiaria() {
        if (!bonusMetaOtorgadoHoy && hoyAtendidos.get() >= eco.getClientesObjetivoDia()) {
            bonusMetaOtorgadoHoy = true;
            eco.otorgarBonusMeta();
            if (paseBatalla != null) paseBatalla.agregarXP(PaseBatalla.XP_META_DIARIA);
            if (gui != null) {
                javax.swing.SwingUtilities.invokeLater(() ->
                    gui.mostrarBonusMeta(eco.getBonusMetaDia()));
            }
        }
    }

    private void iniciarBalanceador() {
        Thread t = new Thread(() -> {
            while (!terminado) {
                try {
                    Thread.sleep(2000);
                    if (terminado) break;
                    List<Cajero> ab = getCajerosAbiertos();
                    if (ab.size() < 2) continue;
                    for (Cajero src : ab) {
                        if (src.getTamanioCola() < 2) continue;
                        Cajero dst = ab.stream().filter(c -> c != src)
                            .min(Comparator.comparingInt(Cajero::getTamanioCola)).orElse(null);
                        if (dst != null && src.getTamanioCola() - dst.getTamanioCola() >= 2) {
                            Cliente mov = src.quitarUltimoCliente();
                            if (mov != null) dst.agregarCliente(mov);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); break;
                }
            }
        }, "Balanceador");
        t.setDaemon(true);
        t.start();
    }

    private void iniciarCicloDia() {
        Thread t = new Thread(() -> {
            while (!terminado) {
                try {
                    Thread.sleep(DURACION_DIA);
                    if (terminado) break;
                    nuevoDia();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); break;
                }
            }
        }, "CicloDia");
        t.setDaemon(true);
        t.start();
    }

    private synchronized void nuevoDia() {
        int diaAnterior  = eco.getDia();
        long ganancias   = eco.getGananciasHoy();
        long robado      = eco.getRobadoHoy();
        int  atrapados   = ladronesAtrapados;
        int  robaron     = ladronesRobaron;
        eco.nuevoDia();
        hoyAtendidos.set(0);
        bonusMetaOtorgadoHoy = false;
        ladronesAtrapados = 0;
        ladronesRobaron   = 0;
        sincronizarCajeros();
        if (gui != null) {
            gui.mostrarCambioDia(diaAnterior, eco.getDia(), ganancias, robado, atrapados, robaron);
            gui.onNuevoDia();
        }
        if (hiloGenerador != null) hiloGenerador.interrupt();
        iniciarGeneradorClientes();
    }

    public synchronized void sincronizarCajeros() {
        int obj = eco.getMaxCajeros();
        while (cajeros.size() < obj) {
            int idx = cajeros.size();
            Cajero c = new Cajero(
                NOMBRES_CAJERO[idx % NOMBRES_CAJERO.length],
                false, this, eco);
            cajeros.add(c);
            c.iniciar();
            if (gui != null) gui.onCajeroAgregado(c);
        }
        if (eco.getNivelVelocidad() >= 3 && cajeros.stream().noneMatch(Cajero::isRapido))
            cajeros.get(0).setRapido(true);
    }

    public void clienteAtendido(Cliente cl, Cajero caj) {
        if (terminado) return;

        // Si es ladrón y no fue atrapado: roba
        if (cl instanceof Ladron l && !l.isAtrapado() && !l.isEliminado()) {
            ladronRobo(l);
            return;
        }

        int tot = totalAtendidos.incrementAndGet();
        hoyAtendidos.incrementAndGet();
        eco.agregarMonedas(eco.getMonedasPorCliente(cl.isVip()));
        sonido.sonarClienteAtendido();
        // XP por cliente atendido
        if (paseBatalla != null)
            paseBatalla.agregarXP(cl.isVip() ? PaseBatalla.XP_CLIENTE_VIP : PaseBatalla.XP_CLIENTE_NORMAL);

        if (gui != null) {
            int cajIdx = cajeros.indexOf(caj);
            gui.iniciarFadeCliente(cl, Math.max(cajIdx, 0));
        }

        // Verificar meta diaria
        verificarMetaDiaria();

        if (tot >= META_CLIENTES) terminar();
    }

    public void redirigirCliente(Cliente cl, Cajero actual) {
        if (terminado) return;
        List<Cajero> otros = getCajerosAbiertos().stream()
            .filter(c -> c != actual).collect(Collectors.toList());
        if (!otros.isEmpty()) {
            Cajero dst = otros.stream()
                .min(Comparator.comparingInt(Cajero::getTamanioCola)).orElse(otros.get(0));
            dst.agregarCliente(cl);
        } else {
            new Thread(() -> {
                try { Thread.sleep(500); redirigirCliente(cl, actual); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }).start();
        }
    }

    private List<Cajero> getCajerosAbiertos() {
        return cajeros.stream().filter(Cajero::isAbierto).collect(Collectors.toList());
    }

    private void terminar() {
        if (terminado) return;
        terminado = true;
        cajeros.forEach(Cajero::detener);
        sonido.sonarFin();
        if (gui != null) gui.ofrecerPrestigio();
    }

    /** Click manual: atiende al primer cliente en cola del cajero más cercano al click */
    public boolean atenderManual(int mouseX, int mouseY) {
        if (prestige == null || !prestige.tieneClickManual()) return false;
        // Buscar el cliente más cercano en cualquier cola
        Cliente candidato = null;
        double minDist = 50 * 50;
        for (Cajero c : cajeros) {
            for (Cliente cl : new java.util.ArrayList<>(c.getCola())) {
                if (cl instanceof Ladron) continue;
                double dx = mouseX - (cl.getX() + 36);
                double dy = mouseY - (cl.getY() + 23);
                double dist = dx*dx + dy*dy;
                if (dist < minDist) { minDist = dist; candidato = cl; }
            }
        }
        if (candidato == null) return false;
        // Removerlo de la cola y atenderlo con 50% de monedas
        final Cliente cl = candidato;
        for (Cajero c : cajeros) c.getCola().remove(cl);
        int tot = totalAtendidos.incrementAndGet();
        hoyAtendidos.incrementAndGet();
        long monedas = Math.round(eco.getMonedasPorCliente(cl.isVip()) * 0.5);
        eco.agregarMonedas(monedas);
        if (paseBatalla != null)
            paseBatalla.agregarXP(cl.isVip() ? PaseBatalla.XP_CLIENTE_VIP : PaseBatalla.XP_CLIENTE_NORMAL);
        if (gui != null) {
            int cajIdx = 0;
            for (int i = 0; i < cajeros.size(); i++) {
                if (cajeros.get(i).getCola().contains(cl)) { cajIdx = i; break; }
            }
            gui.iniciarFadeCliente(cl, cajIdx);
        }
        verificarMetaDiaria();
        if (tot >= META_CLIENTES) terminar();
        return true;
    }

    public List<Cajero>  getCajeros()               { return cajeros; }
    public int  getClientesAtendidosTotal()          { return totalAtendidos.get(); }
    public int  getClientesAtendidosHoy()            { return hoyAtendidos.get(); }
    public int  getMetaClientes()                    { return META_CLIENTES; }
    public boolean isTerminado()                     { return terminado; }
    public SonidoManager getSonido()                 { return sonido; }
    public long getDuracionDia()                     { return DURACION_DIA; }
    public List<Ladron> getLadronesActivos()         { return ladronesActivos; }
    public PaseBatalla  getPaseBatalla()             { return paseBatalla; }
    public PrestigioManager getPrestigio()           { return prestige; }
    public Economia getEco()                         { return eco; }
}

