import java.util.concurrent.LinkedBlockingQueue;
import java.util.Random;

public class Cajero implements Runnable {
    private static final Random random = new Random();

    private String nombre;
    private volatile boolean abierto;
    private boolean rapido;
    private LinkedBlockingQueue<Cliente> cola;
    private Thread hiloAtencion;
    private volatile boolean corriendo;
    private SimulacionBanco simulacion;

    // Cliente que está siendo atendido ACTUALMENTE (fuera de la cola)
    private volatile Cliente clienteEnAtencion = null;

    private int tiempoAtencionMin;
    private int tiempoAtencionMax;

    private double x;
    private double y;

    public Cajero(String nombre, boolean rapido, SimulacionBanco simulacion) {
        this.nombre = nombre;
        this.rapido = rapido;
        this.simulacion = simulacion;
        this.cola = new LinkedBlockingQueue<>();
        this.abierto = true;
        this.corriendo = false;

        if (rapido) {
            this.tiempoAtencionMin = 1500;
            this.tiempoAtencionMax = 2500;
        } else {
            this.tiempoAtencionMin = 2000;
            this.tiempoAtencionMax = 5000;
        }
    }

    public void iniciar() {
        corriendo = true;

        hiloAtencion = new Thread(this, "Cajero-" + nombre);
        hiloAtencion.setDaemon(true);
        hiloAtencion.start();

        Thread estadoHilo = new Thread(() -> {
            while (corriendo && !simulacion.isTerminado()) {
                try {
                    int espera = 3000 + random.nextInt(7000);
                    Thread.sleep(espera);
                    if (!corriendo || simulacion.isTerminado()) break;
                    cerrar();
                    int tiempoCerrado = 2000 + random.nextInt(3000);
                    Thread.sleep(tiempoCerrado);
                    if (!corriendo || simulacion.isTerminado()) break;
                    abrir();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Estado-" + nombre);
        estadoHilo.setDaemon(true);
        estadoHilo.start();
    }

    @Override
    public void run() {
        while (corriendo && !simulacion.isTerminado()) {
            try {
                // take() bloquea limpiamente hasta que haya cliente
                Cliente cliente = cola.take();

                if (!corriendo || simulacion.isTerminado()) break;

                // Si el cajero cerró mientras esperaba, redirigir
                if (!abierto) {
                    simulacion.redirigirCliente(cliente, this);
                    continue;
                }

                // Marcar cliente activo ANTES de dormir
                clienteEnAtencion = cliente;

                int tiempoAtencion = tiempoAtencionMin + random.nextInt(tiempoAtencionMax - tiempoAtencionMin);
                Thread.sleep(tiempoAtencion);

                if (!simulacion.isTerminado()) {
                    cliente.setAtendido(true);
                    cliente.setDesapareciendo(true);
                    simulacion.clienteAtendido(cliente, this);
                }

                clienteEnAtencion = null;

            } catch (InterruptedException e) {
                // Si fue interrumpido por cerrar(), clienteEnAtencion ya fue puesto a null
                // y el cliente fue redirigido por cerrar(). Solo limpiamos y continuamos.
                clienteEnAtencion = null;
                Thread.interrupted(); // limpiar flag de interrupción para seguir el bucle
                // No hacemos break: el hilo sigue corriendo para atender futuros clientes
            }
        }
    }

    public void abrir() {
        this.abierto = true;
        System.out.println("[CAJERO] " + nombre + " ABIERTO");
    }

    public void cerrar() {
        this.abierto = false;
        System.out.println("[CAJERO] " + nombre + " CERRADO - " + cola.size() + " en cola");

        // Redirigir clientes en cola
        java.util.List<Cliente> pendientes = new java.util.ArrayList<>();
        cola.drainTo(pendientes);
        for (Cliente c : pendientes) {
            simulacion.redirigirCliente(c, this);
        }

        // Redirigir también al cliente que estaba siendo atendido
        if (clienteEnAtencion != null) {
            Cliente enAtencion = clienteEnAtencion;
            clienteEnAtencion = null;
            if (hiloAtencion != null) hiloAtencion.interrupt();
            simulacion.redirigirCliente(enAtencion, this);
        }
    }

    public void agregarCliente(Cliente cliente) {
        cola.offer(cliente);
    }

    // Quita el último cliente de la cola (el más alejado del cajero) para balanceo
    public Cliente quitarUltimoCliente() {
        if (cola.isEmpty()) return null;
        java.util.List<Cliente> lista = new java.util.ArrayList<>(cola);
        Cliente ultimo = lista.get(lista.size() - 1);
        cola.remove(ultimo);
        return ultimo;
    }

    public void detener() {
        corriendo = false;
        clienteEnAtencion = null;
        if (hiloAtencion != null) hiloAtencion.interrupt();
    }

    public String getNombre() { return nombre; }
    public boolean isAbierto() { return abierto; }
    public boolean isRapido() { return rapido; }
    public LinkedBlockingQueue<Cliente> getCola() { return cola; }
    public int getTamañoCola() { return cola.size(); }
    public Cliente getClienteEnAtencion() { return clienteEnAtencion; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
}
