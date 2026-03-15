import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PaseBatalla: sistema de XP, niveles (1-20) y recompensas.
 * Hilo-seguro. Se notifica a la GUI cuando el jugador sube de nivel.
 */
public class PaseBatalla {

    public static final int MAX_NIVEL = 20;

    // ── XP ganada por acción ──
    public static final int XP_CLIENTE_NORMAL  = 10;
    public static final int XP_CLIENTE_VIP     = 25;
    public static final int XP_LADRON_ATRAPADO = 15;
    public static final int XP_META_DIARIA     = 50;

    // ── Recompensas por nivel (índice 0 = nivel 1) ──
    public enum TipoRecompensa { MONEDAS, MEJORA_VELOCIDAD, MEJORA_FLUJO, CAJERO_EXTRA, EVENTO_HORA_PICO, EVENTO_DIA_VIP }

    public static class Recompensa {
        public final TipoRecompensa tipo;
        public final long valorMonedas;
        public final String descripcion;

        public Recompensa(TipoRecompensa tipo, long valorMonedas, String descripcion) {
            this.tipo         = tipo;
            this.valorMonedas = valorMonedas;
            this.descripcion  = descripcion;
        }
    }

    private static final Recompensa[] RECOMPENSAS = {
        new Recompensa(TipoRecompensa.MONEDAS,           50,  "50 monedas"),
        new Recompensa(TipoRecompensa.CAJERO_EXTRA,       0,  "Cajero extra desbloqueado"),
        new Recompensa(TipoRecompensa.MONEDAS,           100, "100 monedas"),
        new Recompensa(TipoRecompensa.MEJORA_VELOCIDAD,   0,  "Mejora de velocidad gratis"),
        new Recompensa(TipoRecompensa.EVENTO_HORA_PICO,   0,  "Evento: Hora Pico (30s)"),
        new Recompensa(TipoRecompensa.MONEDAS,           150, "150 monedas"),
        new Recompensa(TipoRecompensa.MEJORA_FLUJO,       0,  "Mejora de flujo gratis"),
        new Recompensa(TipoRecompensa.MONEDAS,           200, "200 monedas"),
        new Recompensa(TipoRecompensa.CAJERO_EXTRA,       0,  "Cajero extra desbloqueado"),
        new Recompensa(TipoRecompensa.EVENTO_DIA_VIP,     0,  "Evento: Día VIP (60s)"),
        new Recompensa(TipoRecompensa.MONEDAS,           250, "250 monedas"),
        new Recompensa(TipoRecompensa.MEJORA_VELOCIDAD,   0,  "Mejora de velocidad gratis"),
        new Recompensa(TipoRecompensa.MONEDAS,           300, "300 monedas"),
        new Recompensa(TipoRecompensa.MEJORA_FLUJO,       0,  "Mejora de flujo gratis"),
        new Recompensa(TipoRecompensa.EVENTO_HORA_PICO,   0,  "Evento: Hora Pico (45s)"),
        new Recompensa(TipoRecompensa.MONEDAS,           400, "400 monedas"),
        new Recompensa(TipoRecompensa.CAJERO_EXTRA,       0,  "Cajero extra desbloqueado"),
        new Recompensa(TipoRecompensa.MONEDAS,           500, "500 monedas"),
        new Recompensa(TipoRecompensa.MEJORA_VELOCIDAD,   0,  "Mejora de velocidad gratis"),
        new Recompensa(TipoRecompensa.MONEDAS,          1000, "¡1000 monedas! Nivel Máximo"),
    };

    // ── XP necesaria por nivel (curva progresiva) ──
    // xpParaNivel[i] = XP total necesaria para alcanzar nivel i+2 (desde nivel i+1)
    private static final int[] XP_POR_NIVEL = {
        100, 200, 350, 550, 800,        // niveles 1→2, 2→3, ..., 5→6
        1100, 1500, 2000, 2600, 3300,   // niveles 6→7, ..., 10→11
        4100, 5000, 6000, 7200, 8500,   // niveles 11→12, ..., 15→16
        10000, 11700, 13600, 15700, 18000 // niveles 16→17, ..., 20 (max)
    };

    private final AtomicLong   xpTotal    = new AtomicLong(0);
    private final AtomicInteger nivelActual = new AtomicInteger(1);

    // XP acumulada dentro del nivel actual
    private volatile long xpEnNivelActual = 0;

    // Callback para notificar a la GUI cuando se sube de nivel
    private Runnable onSubirNivel;
    private volatile int ultimoNivelSubido = 0; // para que la GUI sepa a qué nivel subió

    // Referencia a Economia para aplicar recompensas automáticamente
    private Economia eco;

    public PaseBatalla(Economia eco) {
        this.eco = eco;
    }

    public void setOnSubirNivel(Runnable r) { this.onSubirNivel = r; }

    /** Agrega XP y procesa subidas de nivel */
    public synchronized void agregarXP(long cantidad) {
        if (nivelActual.get() >= MAX_NIVEL) return;

        xpTotal.addAndGet(cantidad);
        xpEnNivelActual += cantidad;

        // Verificar si sube de nivel (puede subir varios a la vez)
        while (nivelActual.get() < MAX_NIVEL) {
            int nivel = nivelActual.get();
            long xpNecesaria = XP_POR_NIVEL[nivel - 1];
            if (xpEnNivelActual >= xpNecesaria) {
                xpEnNivelActual -= xpNecesaria;
                nivelActual.incrementAndGet();
                ultimoNivelSubido = nivelActual.get();
                aplicarRecompensa(ultimoNivelSubido);
                if (onSubirNivel != null) {
                    final Runnable cb = onSubirNivel;
                    new Thread(cb).start();
                }
            } else {
                break;
            }
        }
    }

    private void aplicarRecompensa(int nivel) {
        if (nivel < 1 || nivel > MAX_NIVEL) return;
        Recompensa r = RECOMPENSAS[nivel - 1];
        switch (r.tipo) {
            case MONEDAS -> eco.agregarMonedas(r.valorMonedas);
            case MEJORA_VELOCIDAD -> eco.mejorarVelocidad();
            case MEJORA_FLUJO     -> eco.mejorarFlujo();
            case CAJERO_EXTRA     -> eco.mejorarCajeros();
            case EVENTO_HORA_PICO, EVENTO_DIA_VIP -> { /* Activados por SimulacionBanco */ }
        }
    }

    // ── Getters ──
    public int  getNivel()              { return nivelActual.get(); }
    public long getXpEnNivelActual()    { return xpEnNivelActual; }
    public long getXpParaSiguiente()    {
        int n = nivelActual.get();
        if (n >= MAX_NIVEL) return 0;
        return XP_POR_NIVEL[n - 1];
    }
    public long getXpTotal()            { return xpTotal.get(); }
    public int  getUltimoNivelSubido()  { return ultimoNivelSubido; }
    public boolean esNivelMax()         { return nivelActual.get() >= MAX_NIVEL; }

    public Recompensa getRecompensa(int nivel) {
        if (nivel < 1 || nivel > MAX_NIVEL) return null;
        return RECOMPENSAS[nivel - 1];
    }

    public static int[] getXpPorNivel() { return XP_POR_NIVEL; }
}
