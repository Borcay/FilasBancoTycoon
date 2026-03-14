import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SimulacionBanco {

    private static final int META_CLIENTES  = 200;
    private static final long DURACION_DIA  = 60_000;
    private static final Random random      = new Random();

    private static final String[] NOMBRES_CAJERO =
        {"Pata","Patito","Mano","Manita","Beto","Caro","Tito","Lina"};

    private final List<Cajero> cajeros = new ArrayList<>();
    private final AtomicInteger totalAtendidos = new AtomicInteger(0);
    private final AtomicInteger hoyAtendidos   = new AtomicInteger(0);

    private volatile boolean terminado = false;
    private final Economia eco;
    private InterfazGrafica gui;
    private final SonidoManager sonido;

    private volatile Thread hiloGenerador;

    public SimulacionBanco(Economia eco) {
        this.eco    = eco;
        this.sonido = new SonidoManager();
        inicializarCajeros(eco.getMaxCajeros());
    }

    private void inicializarCajeros(int n) {
        int rapidoIdx = random.nextInt(n);
        for (int i = 0; i < n; i++) {
            cajeros.add(new Cajero(
                NOMBRES_CAJERO[i % NOMBRES_CAJERO.length],
                i == rapidoIdx, this, eco));
        }
    }

    public void setGui(InterfazGrafica g) { this.gui = g; }

    public void iniciar() {
        sonido.iniciarMusicaFondo();
        cajeros.forEach(Cajero::iniciar);
        iniciarGeneradorClientes();
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
                    if (gui != null) gui.repaint();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); break;
                }
            }
        }, "Generador");
        hiloGenerador.setDaemon(true);
        hiloGenerador.start();
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
        int diaAnterior = eco.getDia();
        long ganancias  = eco.getGananciasHoy();
        eco.nuevoDia();
        hoyAtendidos.set(0);
        sincronizarCajeros();
        if (gui != null) {
            gui.mostrarCambioDia(diaAnterior, eco.getDia(), ganancias);
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
        // marcar primer cajero como rápido si se compró la mejora
        if (eco.getNivelVelocidad() >= 3 && cajeros.stream().noneMatch(Cajero::isRapido))
            cajeros.get(0).setRapido(true);
    }

    public void clienteAtendido(Cliente cl, Cajero caj) {
        if (terminado) return;
        int tot = totalAtendidos.incrementAndGet();
        hoyAtendidos.incrementAndGet();
        eco.agregarMonedas(eco.getMonedasPorCliente(cl.isVip()));
        sonido.sonarClienteAtendido();

        // Notificar GUI para fade + coin popup
        if (gui != null) {
            int cajIdx = cajeros.indexOf(caj);
            gui.iniciarFadeCliente(cl, Math.max(cajIdx, 0));
        }

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
        if (gui != null) gui.mostrarFin();
    }

    public List<Cajero> getCajeros()              { return cajeros; }
    public int  getClientesAtendidosTotal()        { return totalAtendidos.get(); }
    public int  getClientesAtendidosHoy()          { return hoyAtendidos.get(); }
    public int  getMetaClientes()                  { return META_CLIENTES; }
    public boolean isTerminado()                   { return terminado; }
    public SonidoManager getSonido()               { return sonido; }
    public long getDuracionDia()                   { return DURACION_DIA; }
}
