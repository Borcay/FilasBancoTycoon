import java.util.concurrent.atomic.AtomicInteger;

public class PrestigioManager {

    /**
     * Árbol de habilidades con sistema de visibilidad por padre.
     *
     * Estructura:
     *   ARBOL_DINERO (raíz, gratis)
     *   ├── BONUS_MONEDAS      (x1.5 monedas)
     *   └── REPUTACION         (VIP nivel 1 al prestigiar)
     *
     * Habilidades anteriores (ahora hijos de BONUS_MONEDAS o REPUTACION):
     *   DESCUENTO_MEJORAS, MEJORAS_INICIALES, MENOS_LADRONES, CLICK_MANUAL
     *
     * Para añadir un nodo nuevo:
     *   1. Añade entrada al enum con (nombre, desc, costo, PADRE)
     *   2. Añade posición en VentanaArbolHabilidades.BASE_POS
     *   3. Añade case en InterfazGrafica.generarEfectoMejora()
     *   4. Añade efecto en los métodos derivados de abajo
     */
    public enum Habilidad {
        // Raíz
        ARBOL_DINERO(     "Arbol de Dinero",    "Desbloquea el arbol de dinero",                   0,  null),
        // Hijos directos del raíz
        BONUS_MONEDAS(    "Flujo de Riqueza",   "Todos los bancos ganan x1.5 monedas",             2,  ARBOL_DINERO),
        REPUTACION(       "Reputacion",         "Al prestigiar, el banco conserva la mejora VIP en nivel 1", 2, ARBOL_DINERO),
        // Hijos de BONUS_MONEDAS
        DESCUENTO_MEJORAS("Gestion Eficiente",  "Reduce el costo de todas las mejoras un 20%",     2,  BONUS_MONEDAS),
        MEJORAS_INICIALES("Herencia",           "Cada prestige comienza con velocidad y flujo nivel 1", 3, BONUS_MONEDAS),
        // Hijos de REPUTACION
        MENOS_LADRONES(   "Guardia Experto",    "Los ladrones aparecen un 50% menos seguido",      2,  REPUTACION),
        CLICK_MANUAL(     "Atencion Directa",   "Click en un cliente en cola lo atiende (50% monedas)", 4, REPUTACION);

        public final String    nombre;
        public final String    descripcion;
        public final int       costoBilletes;
        public final Habilidad padre; // null = raíz

        Habilidad(String nombre, String descripcion, int costo, Habilidad padre) {
            this.nombre        = nombre;
            this.descripcion   = descripcion;
            this.costoBilletes = costo;
            this.padre         = padre;
        }

        public boolean esRaiz() { return padre == null; }
    }

    // ── Estado ────────────────────────────────────────────────────────────
    private final AtomicInteger billetes   = new AtomicInteger(0);
    private volatile int        prestigios = 0;
    private final boolean[] desbloqueadas  = new boolean[Habilidad.values().length];
    private volatile int numeroBancos      = 1;

    // ── Billetes ──────────────────────────────────────────────────────────
    public int  getBilletes()              { return billetes.get(); }
    public void agregarBilletes(int n)     { billetes.addAndGet(n); }

    public synchronized boolean gastarSync(int cantidad) {
        if (billetes.get() < cantidad) return false;
        billetes.addAndGet(-cantidad);
        return true;
    }

    // ── Prestigios ────────────────────────────────────────────────────────
    public int  getPrestigios()            { return prestigios; }
    public void incrementarPrestigios()    { prestigios++; }
    public int  billetesDelPrestige()      { return 1; }

    // ── Árbol ─────────────────────────────────────────────────────────────
    public boolean estaDesbloqueada(Habilidad h) {
        return desbloqueadas[h.ordinal()];
    }

    /**
     * Un nodo es VISIBLE si:
     *  - Es la raíz, o
     *  - Su padre está desbloqueado
     */
    public boolean esVisible(Habilidad h) {
        if (h.esRaiz()) return true;
        return estaDesbloqueada(h.padre);
    }

    /**
     * Un nodo se puede COMPRAR si:
     *  - Es visible
     *  - No está ya desbloqueado
     *  - Tiene suficientes billetes
     */
    public boolean sePuedeComprar(Habilidad h) {
        return esVisible(h) && !estaDesbloqueada(h) && billetes.get() >= h.costoBilletes;
    }

    public boolean desbloquear(Habilidad h) {
        if (estaDesbloqueada(h)) return false;
        if (h.costoBilletes > 0 && !gastarSync(h.costoBilletes)) return false;
        desbloqueadas[h.ordinal()] = true;
        return true;
    }

    // ── Bancos ────────────────────────────────────────────────────────────
    public int  getNumeroBancos() { return numeroBancos; }
    public int  costoBanco()      { return (int) Math.pow(2, numeroBancos - 1); }

    public boolean comprarBanco() {
        if (!gastarSync(costoBanco())) return false;
        numeroBancos++;
        return true;
    }

    // ── Efectos derivados ─────────────────────────────────────────────────
    public double multiplicadorMonedas() {
        return estaDesbloqueada(Habilidad.BONUS_MONEDAS) ? 1.5 : 1.0;
    }

    public double multiplicadorCostoMejoras() {
        return estaDesbloqueada(Habilidad.DESCUENTO_MEJORAS) ? 0.80 : 1.0;
    }

    public boolean tieneMejorasIniciales() {
        return estaDesbloqueada(Habilidad.MEJORAS_INICIALES);
    }

    public double multiplicadorIntervaloLadrones() {
        return estaDesbloqueada(Habilidad.MENOS_LADRONES) ? 2.0 : 1.0;
    }

    public boolean tieneClickManual() {
        return estaDesbloqueada(Habilidad.CLICK_MANUAL);
    }

    public boolean tieneReputacion() {
        return estaDesbloqueada(Habilidad.REPUTACION);
    }

    // ── PaseBatalla global ────────────────────────────────────────────────
    private PaseBatalla paseBatallaGlobal;
    public PaseBatalla getPaseBatallaGlobal() {
        if (paseBatallaGlobal == null) paseBatallaGlobal = new PaseBatalla(null);
        return paseBatallaGlobal;
    }
    public void setPaseBatallaGlobal(PaseBatalla p) { paseBatallaGlobal = p; }
}
