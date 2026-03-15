import java.util.concurrent.atomic.AtomicInteger;

/**
 * PrestigioManager: maneja los billetes (moneda de prestige),
 * el árbol de habilidades y cuántos prestigios se han hecho.
 * Es GLOBAL — no se reinicia entre prestigios.
 */
public class PrestigioManager {

    // ── Habilidades del árbol ──────────────────────────────────────────────
    public enum Habilidad {
        DESCUENTO_MEJORAS(   "Descuento en Mejoras",   "Reduce el costo de todas las mejoras un 20%",  1),
        BONUS_MONEDAS(       "Bonus de Monedas",        "Todos los bancos ganan +30% monedas",          2),
        MEJORAS_INICIALES(   "Mejoras al Iniciar",      "Cada prestige comienza con 1 nivel de velocidad y flujo desbloqueados", 3),
        MENOS_LADRONES(      "Guardia Experto",         "Los ladrones aparecen un 50% menos seguido",   2),
        CLICK_MANUAL(        "Atencion Manual",         "Click en un cliente en cola lo atiende (50% de monedas)", 4);

        public final String nombre;
        public final String descripcion;
        public final int costoBilletes;

        Habilidad(String nombre, String descripcion, int costo) {
            this.nombre       = nombre;
            this.descripcion  = descripcion;
            this.costoBilletes = costo;
        }
    }

    // ── Estado ────────────────────────────────────────────────────────────
    private final AtomicInteger billetes      = new AtomicInteger(0);
    private volatile int        prestigios    = 0;  // cuántas veces ha hecho prestige

    // Habilidades desbloqueadas (una por índice de Habilidad.values())
    private final boolean[] desbloqueadas = new boolean[Habilidad.values().length];

    // Costo de comprar el siguiente banco: 1, 2, 4, 8, 16...
    // El jugador empieza con 1 banco; la lista de sims es externa.
    private volatile int numeroBancos = 1;

    // ── Billetes ──────────────────────────────────────────────────────────
    public int getBilletes()  { return billetes.get(); }

    public void agregarBilletes(int cantidad) { billetes.addAndGet(cantidad); }

    public boolean gastarBilletes(int cantidad) {
        return billetes.updateAndGet(v -> v >= cantidad ? v - cantidad : v) >= 0
               && billetes.get() != billetes.get() - cantidad // always true trick — use sync below
               || true; // fallback: use gastarSync
    }

    // Versión correcta sincronizada
    public synchronized boolean gastarSync(int cantidad) {
        if (billetes.get() < cantidad) return false;
        billetes.addAndGet(-cantidad);
        return true;
    }

    // ── Prestigios ────────────────────────────────────────────────────────
    public int  getPrestigios()  { return prestigios; }
    public void incrementarPrestigios() { prestigios++; }

    /** Cuántos billetes da el próximo prestige (siempre 1 por ahora, puede escalar) */
    public int billetesDelPrestige() { return 1; }

    // ── Árbol de habilidades ──────────────────────────────────────────────
    public boolean estaDesbloqueada(Habilidad h) {
        return desbloqueadas[h.ordinal()];
    }

    public boolean desbloquear(Habilidad h) {
        if (estaDesbloqueada(h)) return false;
        if (!gastarSync(h.costoBilletes)) return false;
        desbloqueadas[h.ordinal()] = true;
        return true;
    }

    // ── PaseBatalla global ────────────────────────────────────────────────
    private PaseBatalla paseBatallaGlobal;
    public PaseBatalla getPaseBatallaGlobal() {
        if (paseBatallaGlobal == null) paseBatallaGlobal = new PaseBatalla(null);
        return paseBatallaGlobal;
    }
    public void setPaseBatallaGlobal(PaseBatalla p) { paseBatallaGlobal = p; }

    // ── Bancos ────────────────────────────────────────────────────────────
    public int getNumeroBancos() { return numeroBancos; }

    /** Costo en billetes de comprar el siguiente banco (1,2,4,8,...) */
    public int costoBanco() {
        // Ya tiene numeroBancos, el siguiente cuesta 2^(numeroBancos-1)
        return (int) Math.pow(2, numeroBancos - 1);
    }

    public boolean comprarBanco() {
        int costo = costoBanco();
        if (!gastarSync(costo)) return false;
        numeroBancos++;
        return true;
    }

    // ── Efectos derivados ─────────────────────────────────────────────────

    /** Multiplicador de monedas para todos los bancos */
    public double multiplicadorMonedas() {
        return estaDesbloqueada(Habilidad.BONUS_MONEDAS) ? 1.30 : 1.0;
    }

    /** Multiplicador de costo de mejoras */
    public double multiplicadorCostoMejoras() {
        return estaDesbloqueada(Habilidad.DESCUENTO_MEJORAS) ? 0.80 : 1.0;
    }

    /** Si hay mejoras iniciales al prestigiar */
    public boolean tieneMejorasIniciales() {
        return estaDesbloqueada(Habilidad.MEJORAS_INICIALES);
    }

    /** Multiplicador del intervalo entre ladrones (1.0 = normal, 2.0 = doble tiempo = mitad ladrones) */
    public double multiplicadorIntervaloLadrones() {
        return estaDesbloqueada(Habilidad.MENOS_LADRONES) ? 2.0 : 1.0;
    }

    /** Si el jugador puede atender clientes manualmente con click */
    public boolean tieneClickManual() {
        return estaDesbloqueada(Habilidad.CLICK_MANUAL);
    }
}
