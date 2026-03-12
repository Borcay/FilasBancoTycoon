import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SimulacionBanco {
    private static final int META_CLIENTES = 50;
    private static final Random random = new Random();

    private List<Cajero> cajeros;
    private AtomicInteger clientesAtendidos; // AtomicInteger evita race condition en el contador
    private volatile boolean terminado;
    private long tiempoInicio;
    private InterfazGrafica gui;

    public SimulacionBanco() {
        this.cajeros = new ArrayList<>();
        this.clientesAtendidos = new AtomicInteger(0);
        this.terminado = false;
        inicializarCajeros();
    }

    private void inicializarCajeros() {
        String[] nombres = {"Pata", "Patito", "Mano", "Manita"};
        int cajeroRapidoIndex = random.nextInt(4);
        for (int i = 0; i < 4; i++) {
            boolean esRapido = (i == cajeroRapidoIndex);
            Cajero cajero = new Cajero(nombres[i], esRapido, this);
            cajeros.add(cajero);
            if (esRapido) System.out.println("[SISTEMA] Cajero rápido: " + nombres[i]);
        }
    }

    public void setGui(InterfazGrafica gui) {
        this.gui = gui;
    }

    public void iniciar() {
        tiempoInicio = System.currentTimeMillis();
        for (Cajero c : cajeros) c.iniciar();
        iniciarGeneradorClientes();
        iniciarBalanceador();
    }

    private void iniciarGeneradorClientes() {
        Thread generador = new Thread(() -> {
            while (!terminado) {
                try {
                    int intervalo = 1000 + random.nextInt(2000);
                    Thread.sleep(intervalo);
                    if (terminado) break;

                    List<Cajero> abiertos = getCajerosAbiertos();
                    if (!abiertos.isEmpty()) {
                        Cliente cliente = new Cliente();
                        Cajero destino = abiertos.get(random.nextInt(abiertos.size()));
                        destino.agregarCliente(cliente);
                        System.out.println("[NUEVO] " + cliente.getNombre() + " -> " + destino.getNombre());
                        if (gui != null) gui.repaint();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "GeneradorClientes");
        generador.setDaemon(true);
        generador.start();
    }

    private void iniciarBalanceador() {
        Thread balanceador = new Thread(() -> {
            while (!terminado) {
                try {
                    // Revisar cada 2 segundos si hay filas desbalanceadas
                    Thread.sleep(2000);
                    if (terminado) break;

                    List<Cajero> abiertos = getCajerosAbiertos();
                    if (abiertos.size() < 2) continue;

                    for (Cajero cajeroOrigen : abiertos) {
                        // Solo mover clientes que están en cola (no al que se está atendiendo)
                        int tamOrigen = cajeroOrigen.getTamañoCola();
                        if (tamOrigen == 0) continue;

                        // Buscar el cajero abierto con menor cola
                        Cajero cajeroDestino = abiertos.stream()
                                .filter(c -> !c.equals(cajeroOrigen))
                                .min((a, b) -> a.getTamañoCola() - b.getTamañoCola())
                                .orElse(null);

                        if (cajeroDestino == null) continue;

                        int tamDestino = cajeroDestino.getTamañoCola();

                        // Solo mover si la diferencia es de al menos 2 clientes
                        if (tamOrigen - tamDestino >= 2) {
                            Cliente clienteAMover = cajeroOrigen.quitarUltimoCliente();
                            if (clienteAMover != null) {
                                cajeroDestino.agregarCliente(clienteAMover);
                                System.out.println("[BALANCEO] " + clienteAMover.getNombre()
                                        + " : " + cajeroOrigen.getNombre()
                                        + " -> " + cajeroDestino.getNombre()
                                        + " (" + tamOrigen + " vs " + tamDestino + ")");
                                if (gui != null) gui.repaint();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Balanceador");
        balanceador.setDaemon(true);
        balanceador.start();
    }

    // Sin synchronized aquí — AtomicInteger lo hace thread-safe solo
    public void clienteAtendido(Cliente cliente, Cajero cajero) {
        if (terminado) return;

        int total = clientesAtendidos.incrementAndGet();
        System.out.println("[ATENDIDO] " + cliente.getNombre() + " por " + cajero.getNombre()
                + " | Total: " + total + "/" + META_CLIENTES);

        if (gui != null) gui.repaint();

        if (total >= META_CLIENTES) {
            terminar();
        }
    }

    public void redirigirCliente(Cliente cliente, Cajero cajeroActual) {
        if (terminado) return;

        List<Cajero> abiertos = getCajerosAbiertos().stream()
                .filter(c -> !c.equals(cajeroActual))
                .collect(Collectors.toList());

        if (!abiertos.isEmpty()) {
            Cajero destino = abiertos.stream()
                    .min((a, b) -> a.getTamañoCola() - b.getTamañoCola())
                    .orElse(abiertos.get(0));
            destino.agregarCliente(cliente);
            System.out.println("[REDIRIGIDO] " + cliente.getNombre() + " -> " + destino.getNombre());
            if (gui != null) gui.repaint();
        } else {
            // Reintentar en 500ms
            Thread t = new Thread(() -> {
                try {
                    Thread.sleep(500);
                    redirigirCliente(cliente, cajeroActual);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            t.setDaemon(true);
            t.start();
        }
    }

    private List<Cajero> getCajerosAbiertos() {
        return cajeros.stream().filter(Cajero::isAbierto).collect(Collectors.toList());
    }

    private void terminar() {
        if (terminado) return;
        terminado = true;
        for (Cajero c : cajeros) c.detener();
        System.out.println("[FIN] Simulación terminada. 50 clientes atendidos.");
        if (gui != null) gui.mostrarFin();
    }

    public List<Cajero> getCajeros() { return cajeros; }
    public int getClientesAtendidos() { return clientesAtendidos.get(); }
    public int getMetaClientes() { return META_CLIENTES; }
    public boolean isTerminado() { return terminado; }
    public long getTiempoInicio() { return tiempoInicio; }
}
