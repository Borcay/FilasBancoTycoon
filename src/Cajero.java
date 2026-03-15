import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

public class Cajero implements Runnable {

    private static final Random random = new Random();

    private final String nombre;
    private final SimulacionBanco simulacion;
    private final Economia eco;

    private volatile boolean abierto   = true;
    private volatile boolean corriendo = false;
    private boolean rapido;

    private final LinkedBlockingQueue<Cliente> cola = new LinkedBlockingQueue<>();
    private volatile Cliente clienteEnAtencion = null;

    // progreso de atención (0.0 – 1.0), actualizado por el hilo de atención
    private volatile double progresoAtencion = 0.0;

    private Thread hiloAtencion;

    // posición visual del cuadro del cajero
    private double x, y;

    public Cajero(String nombre, boolean rapido, SimulacionBanco simulacion, Economia eco) {
        this.nombre     = nombre;
        this.rapido     = rapido;
        this.simulacion = simulacion;
        this.eco        = eco;
    }

    public void iniciar() {
        corriendo = true;
        hiloAtencion = new Thread(this, "Cajero-" + nombre);
        hiloAtencion.setDaemon(true);
        hiloAtencion.start();
        iniciarCicloEstado();
    }

    /** Ciclo que abre/cierra el cajero aleatoriamente */
    private void iniciarCicloEstado() {
        Thread t = new Thread(() -> {
            while (corriendo && !simulacion.isTerminado()) {
                try {
                    Thread.sleep(8000 + random.nextInt(14000));
                    if (!corriendo || simulacion.isTerminado()) break;
                    cerrar();
                    Thread.sleep(3000 + random.nextInt(5000));
                    if (!corriendo || simulacion.isTerminado()) break;
                    abrir();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Estado-" + nombre);
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void run() {
        while (corriendo && !simulacion.isTerminado()) {
            try {
                Cliente cliente = cola.take();
                if (!corriendo || simulacion.isTerminado()) break;

                // Si el cajero está cerrado al momento de tomar al cliente, redirigir
                if (!abierto) {
                    simulacion.redirigirCliente(cliente, this);
                    continue;
                }

                clienteEnAtencion = cliente;
                progresoAtencion  = 0.0;

                long tiempoBase = eco.getServeBaseMs();
                long tiempoReal = rapido
                    ? (long)(tiempoBase * 0.55)
                    : (long)(tiempoBase * (0.85 + random.nextDouble() * 0.30));

                // Atender en pequeños pasos para poder abortar si cierran el cajero
                int pasos = 60;
                long msPorPaso = tiempoReal / pasos;
                boolean abortado = false;

                for (int i = 0; i < pasos; i++) {
                    if (!corriendo || simulacion.isTerminado() || !abierto) {
                        abortado = true;
                        break;
                    }
                    Thread.sleep(msPorPaso);
                    progresoAtencion = (double)(i + 1) / pasos;
                }

                if (abortado) {
                    // El cajero cerró a mitad: redirigir al cliente
                    Cliente enAtencion = clienteEnAtencion;
                    clienteEnAtencion = null;
                    progresoAtencion  = 0.0;
                    if (enAtencion != null && !simulacion.isTerminado()) {
                        simulacion.redirigirCliente(enAtencion, this);
                    }
                    Thread.interrupted(); // limpiar flag
                    continue;
                }

                // Atención completada
                if (!simulacion.isTerminado()) {
                    cliente.setAtendido(true);
                    cliente.setDesapareciendo(true);
                    simulacion.clienteAtendido(cliente, this);
                }
                clienteEnAtencion = null;
                progresoAtencion  = 0.0;

            } catch (InterruptedException e) {
                clienteEnAtencion = null;
                progresoAtencion  = 0.0;
                Thread.interrupted();
            }
        }
    }

    public void abrir() {
        abierto = true;
    }

    public void cerrar() {
        abierto = false;

        // Vaciar cola y redirigir
        java.util.List<Cliente> pendientes = new java.util.ArrayList<>();
        cola.drainTo(pendientes);
        for (Cliente c : pendientes) simulacion.redirigirCliente(c, this);

        // El cliente en atención se redirigirá cuando el loop detecte !abierto
    }

    public void agregarCliente(Cliente c)  { cola.offer(c); }

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

    /** Bug6: Expulsa al cliente que está siendo atendido (usado para atrapar ladrones mid-atencion) */
    public void expulsarClienteEnAtencion() {
        clienteEnAtencion = null;
        progresoAtencion  = 0.0;
        if (hiloAtencion != null) hiloAtencion.interrupt();
    }

    // Permite actualizar la velocidad cuando se compra la mejora
    public void setRapido(boolean rapido) { this.rapido = rapido; }

    public String  getNombre()             { return nombre; }
    public boolean isAbierto()             { return abierto; }
    public boolean isRapido()              { return rapido; }
    public LinkedBlockingQueue<Cliente> getCola() { return cola; }
    public int     getTamanioCola()        { return cola.size(); }
    public Cliente getClienteEnAtencion()  { return clienteEnAtencion; }
    public double  getProgresoAtencion()   { return progresoAtencion; }

    public double getX() { return x; } public void setX(double v) { x = v; }
    public double getY() { return y; } public void setY(double v) { y = v; }
}
