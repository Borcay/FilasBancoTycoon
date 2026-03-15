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
            Cliente cliente = null;
            try {
                cliente = cola.take();
            } catch (InterruptedException e) {
                Thread.interrupted();
                continue; // volver al while, revisará corriendo
            }

            if (!corriendo || simulacion.isTerminado()) break;

            // Si el cajero está cerrado al tomar al cliente, redirigir inmediatamente
            if (!abierto) {
                simulacion.redirigirCliente(cliente, this);
                continue;
            }

            // Iniciar atención
            clienteEnAtencion = cliente;
            progresoAtencion  = 0.0;

            long tiempoBase = eco.getServeBaseMs();
            long tiempoReal = rapido
                ? (long)(tiempoBase * 0.55)
                : (long)(tiempoBase * (0.85 + random.nextDouble() * 0.30));

            int  pasos      = 60;
            long msPorPaso  = Math.max(1, tiempoReal / pasos);
            boolean exito   = true;

            for (int i = 0; i < pasos; i++) {
                // Abortar si el cajero cerró o la simulación terminó
                if (!corriendo || simulacion.isTerminado() || !abierto) {
                    exito = false;
                    break;
                }
                try {
                    Thread.sleep(msPorPaso);
                } catch (InterruptedException ie) {
                    Thread.interrupted();
                    exito = false;
                    break;
                }
                // Solo actualizar progreso si seguimos abiertos
                if (abierto) progresoAtencion = (double)(i + 1) / pasos;
            }

            // Limpiar siempre antes de decidir qué hacer
            Cliente atendido  = clienteEnAtencion;
            clienteEnAtencion = null;
            progresoAtencion  = 0.0;

            if (!exito) {
                // Cajero cerró o fue interrumpido: redirigir al cliente
                if (atendido != null && !simulacion.isTerminado()) {
                    simulacion.redirigirCliente(atendido, this);
                }
            } else {
                // Atención completada exitosamente
                if (atendido != null && !simulacion.isTerminado()) {
                    atendido.setAtendido(true);
                    atendido.setDesapareciendo(true);
                    simulacion.clienteAtendido(atendido, this);
                }
            }
        }
    }

    public void abrir() {
        abierto = true;
        // Despertar el hilo si está bloqueado en cola.take()
        // Interrumpirlo hace que salga del take, limpie el flag y vuelva al while
        if (hiloAtencion != null) hiloAtencion.interrupt();
    }

    public void cerrar() {
        abierto = false;

        // Vaciar cola y redirigir
        java.util.List<Cliente> pendientes = new java.util.ArrayList<>();
        cola.drainTo(pendientes);
        for (Cliente c : pendientes) simulacion.redirigirCliente(c, this);

        // Interrumpir el hilo si está bloqueado en cola.take() o en sleep
        // El catch lo maneja limpiamente: redirige al cliente en atención si había uno
        if (hiloAtencion != null) hiloAtencion.interrupt();
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

    /** Expulsa al cliente siendo atendido (para atrapar ladrones mid-atención) */
    public void expulsarClienteEnAtencion() {
        // Interrumpir PRIMERO — el catch del hilo guarda la referencia y redirige
        if (hiloAtencion != null) hiloAtencion.interrupt();
        progresoAtencion = 0.0;
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