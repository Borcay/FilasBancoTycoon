import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Economía del tycoon: monedas, día, mejoras.
 * Hilo-seguro: usa AtomicLong para monedas.
 */
public class Economia {

    // ── Niveles máximos de cada mejora ──
    public static final int MAX_CAJEROS    = 4;   // niveles extra (total 1+4=5 cajeros)
    public static final int MAX_FLUJO      = 4;
    public static final int MAX_VELOCIDAD  = 4;
    public static final int MAX_MONEDAS    = 4;
    public static final int MAX_VIP        = 3;

    // ── Costo base de cada mejora (se multiplica x 1.7^nivel) ──
    private static final int BASE_CAJEROS   = 120;
    private static final int BASE_FLUJO     = 80;
    private static final int BASE_VELOCIDAD = 100;
    private static final int BASE_MONEDAS   = 60;
    private static final int BASE_VIP       = 200;

    private final AtomicLong monedas = new AtomicLong(0);
    private final AtomicLong gananciasHoy = new AtomicLong(0);
    private final AtomicLong robadoHoy    = new AtomicLong(0);
    private volatile long mejorDia = 0;

    private volatile int dia = 1;
    private volatile long inicioJuego;
    private volatile long inicioDia;

    // ── niveles de mejoras ──
    private volatile int nivelCajeros   = 0;
    private volatile int nivelFlujo     = 0;
    private volatile int nivelVelocidad = 0;
    private volatile int nivelMonedas   = 0;
    private volatile int nivelVip       = 0;

    public Economia() {
        inicioJuego = System.currentTimeMillis();
        inicioDia   = System.currentTimeMillis();
    }

    // ── Monedas ──
    public long getMonedas() { return monedas.get(); }

    public boolean gastar(long cantidad) {
        return monedas.updateAndGet(v -> v >= cantidad ? v - cantidad : v)
               != monedas.get() - cantidad
               ? false
               : true;
        // Versión correcta:
    }

    // versión correcta sin la confusión anterior
    public synchronized boolean gastarSync(long cantidad) {
        if (monedas.get() < cantidad) return false;
        monedas.addAndGet(-cantidad);
        return true;
    }

    public void agregarMonedas(long cantidad) {
        monedas.addAndGet(cantidad);
        gananciasHoy.addAndGet(cantidad);
    }

    public void descontarMonedas(long cantidad) {
        monedas.updateAndGet(v -> Math.max(0, v - cantidad));
        robadoHoy.addAndGet(cantidad);
    }

    public long getGananciasHoy() { return gananciasHoy.get(); }
    public long getRobadoHoy()    { return robadoHoy.get(); }
    public long getMejorDia()     { return mejorDia; }

    // ── Día ──
    public int  getDia()         { return dia; }
    public long getInicioDia()   { return inicioDia; }
    public long getInicioJuego() { return inicioJuego; }

    public void nuevoDia() {
        long ganHoy = gananciasHoy.getAndSet(0);
        if (ganHoy > mejorDia) mejorDia = ganHoy;
        robadoHoy.set(0);
        dia++;
        inicioDia = System.currentTimeMillis();
    }

    // ── Valores derivados ──
    public int getMaxCajeros()      { return 1 + nivelCajeros; }
    public long getSpawnIntervaloMs() { return Math.max(400, 2100 - nivelFlujo * 420L); }
    public long getServeBaseMs()    { return Math.max(900, 4000 - nivelVelocidad * 720L); }
    public int getMonedasPorCliente(boolean vip) {
        int base = 10 + nivelMonedas * 8;
        return vip ? base * 2 : base;
    }
    public double getProbVip() { return nivelVip * 0.10; }
    /** Meta diaria calibrada: número alcanzable según cajeros y velocidad de atención */
    public int getClientesObjetivoDia() {
        int cajeros   = getMaxCajeros();
        long durMs    = 60_000L;
        long tiempoAt = getServeBaseMs();
        int capacidad = (int)(cajeros * (durMs / (double) tiempoAt));
        // Meta = 60% de capacidad teórica + rampa por día y flujo
        int meta = (int)(capacidad * 0.60) + (dia - 1) * 2 + nivelFlujo * 2;
        return Math.max(5, Math.min(meta, 50));
    }

    // ── Bonus meta diaria ──
    private static final long BONUS_META_DIA = 100;
    public long getBonusMetaDia() { return BONUS_META_DIA; }
    public void otorgarBonusMeta() { agregarMonedas(BONUS_META_DIA); }

    // ── Costos de mejoras ──
    public int costoCajeros()   { return costoMejora(BASE_CAJEROS,   nivelCajeros);   }
    public int costoFlujo()     { return costoMejora(BASE_FLUJO,     nivelFlujo);     }
    public int costoVelocidad() { return costoMejora(BASE_VELOCIDAD, nivelVelocidad); }
    public int costoMonedas()   { return costoMejora(BASE_MONEDAS,   nivelMonedas);   }
    public int costoVip()       { return costoMejora(BASE_VIP,       nivelVip);       }

    private int costoMejora(int base, int nivel) {
        return (int) Math.round(base * Math.pow(1.7, nivel));
    }

    // ── Niveles getter/setter ──
    public int getNivelCajeros()   { return nivelCajeros;   }
    public int getNivelFlujo()     { return nivelFlujo;     }
    public int getNivelVelocidad() { return nivelVelocidad; }
    public int getNivelMonedas()   { return nivelMonedas;   }
    public int getNivelVip()       { return nivelVip;       }

    public synchronized boolean mejorarCajeros()   { if (nivelCajeros   >= MAX_CAJEROS   || !gastarSync(costoCajeros()))   return false; nivelCajeros++;   return true; }
    public synchronized boolean mejorarFlujo()     { if (nivelFlujo     >= MAX_FLUJO     || !gastarSync(costoFlujo()))     return false; nivelFlujo++;     return true; }
    public synchronized boolean mejorarVelocidad() { if (nivelVelocidad >= MAX_VELOCIDAD || !gastarSync(costoVelocidad())) return false; nivelVelocidad++; return true; }
    public synchronized boolean mejorarMonedas()   { if (nivelMonedas   >= MAX_MONEDAS   || !gastarSync(costoMonedas()))   return false; nivelMonedas++;   return true; }
    public synchronized boolean mejorarVip()       { if (nivelVip       >= MAX_VIP       || !gastarSync(costoVip()))       return false; nivelVip++;       return true; }
}
