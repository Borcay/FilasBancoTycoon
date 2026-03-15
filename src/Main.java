import javax.swing.*;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Main {

    // ── MODO DEBUG ────────────────────────────────────────────────────────
    // Cambia a true para arrancar con billetes y prestige ya activo.
    // Cambia a false antes de entregar/publicar el juego.
    private static final boolean DEBUG = true;
    // ─────────────────────────────────────────────────────────────────────

    private static final List<SimulacionBanco> todasSims = new ArrayList<>();
    private static final List<Economia>        todasEcos = new ArrayList<>();
    private static final PrestigioManager      prestige  = new PrestigioManager();
    private static InterfazGrafica             guiActual;

    public static void main(String[] args) {
        prestige.setPaseBatallaGlobal(new PaseBatalla(null));

        if (DEBUG) aplicarEstadoDebug();

        SwingUtilities.invokeLater(() -> iniciarNuevoBanco(-1));
    }

    private static void aplicarEstadoDebug() {
        // Simular 1 prestige ya hecho + 10 billetes + monedas iniciales x20
        prestige.incrementarPrestigios();
        prestige.agregarBilletes(10);
        // Las monedas se aplican después de crear la economía — se hace en iniciarNuevoBanco
    }

    private static void aplicarMonedasDebug(Economia eco) {
        eco.agregarMonedas(5000); // x20 de lo normal para probar mejoras rápido
    }

    /**
     * Inicia un banco nuevo.
     * @param indiceAReemplazar -1 = primer arranque o banco adicional,
     *                          >=0 = índice del banco que se prestige y se reinicia
     */
    private static void iniciarNuevoBanco(int indiceAReemplazar) {
        Economia eco = new Economia();
        if (DEBUG) aplicarMonedasDebug(eco);
        SimulacionBanco sim = new SimulacionBanco(eco);
        sim.setPrestigio(prestige);

        boolean esPrestigio = indiceAReemplazar >= 0;

        if (!esPrestigio) {
            todasSims.add(sim);
            todasEcos.add(eco);
        } else {
            // Detener el banco viejo
            SimulacionBanco bancoViejo = todasSims.get(indiceAReemplazar);
            bancoViejo.detenerTodo();
            todasSims.set(indiceAReemplazar, sim);
            todasEcos.set(indiceAReemplazar, eco);
        }

        // Si no hay GUI aún, crearla con el primer banco
        if (guiActual == null) {
            guiActual = new InterfazGrafica(sim, eco);
            sim.setGui(guiActual);
            registrarCallbacks();
        } else if (esPrestigio) {
            // Cambiar la vista al banco recién reiniciado
            sim.setGui(guiActual);
            guiActual.cambiarVistaBanco(sim, eco);
            guiActual.refrescarPanelLateral(new ArrayList<>(todasSims));
            guiActual.mostrarToast("Prestige! +1 Billete. Banco " + (indiceAReemplazar+1) + " reiniciado.", new Color(180,120,255));
        }

        // Activar UI de prestige si ya se ha prestigiado
        if (prestige.getPrestigios() > 0) {
            guiActual.activarUIPrestigioPublico();
            guiActual.refrescarPanelLateral(new ArrayList<>(todasSims));
        }

        if (!guiActual.isVisible()) guiActual.setVisible(true);
        sim.iniciar();
    }

    private static void registrarCallbacks() {
        // Prestige de cualquier banco — recibe el sim que lo disparó
        guiActual.setOnPrestigioConBanco((InterfazGrafica.SimConsumer) simQuePrestigia ->
            SwingUtilities.invokeLater(() -> {
                int idx = todasSims.indexOf(simQuePrestigia);
                if (idx < 0) idx = 0;
                iniciarNuevoBanco(idx);
            }));

        // Cambiar vista al banco del índice
        guiActual.setOnCambiarBanco((InterfazGrafica.BancoIndexConsumer) idx ->
            SwingUtilities.invokeLater(() -> {
                if (idx < 0 || idx >= todasSims.size()) return;
                guiActual.cambiarVistaBanco(todasSims.get(idx), todasEcos.get(idx));
                guiActual.refrescarPanelLateral(new ArrayList<>(todasSims));
            }));

        guiActual.setOnObtenerIndiceBanco((InterfazGrafica.BancoIndexFunction) s -> todasSims.indexOf(s));

        // Refrescar cubos
        guiActual.setOnRefrescarCubos(() ->
            SwingUtilities.invokeLater(() -> guiActual.refrescarPanelLateral(new ArrayList<>(todasSims))));

        // No se usa (panel lateral reemplaza VentanaBancos)
        guiActual.setOnAbrirBancos(() -> {});

        // Comprar banco desde panel lateral
        guiActual.setOnCompraBancoLateral(() -> SwingUtilities.invokeLater(() -> {
            if (prestige.comprarBanco()) {
                agregarNuevoBanco();
                guiActual.mostrarToast("Nuevo banco comprado!", new Color(180, 120, 255));
            } else {
                guiActual.mostrarToast("Necesitas " + prestige.costoBanco() + " B.", new Color(180, 60, 60));
            }
        }));
    }

    private static void agregarNuevoBanco() {
        Economia eco = new Economia();
        SimulacionBanco sim = new SimulacionBanco(eco);
        sim.setPrestigio(prestige);
        todasSims.add(sim);
        todasEcos.add(eco);
        // Sin GUI propia — se ve desde panel lateral
        sim.iniciar();
        if (guiActual != null)
            guiActual.refrescarPanelLateral(new ArrayList<>(todasSims));
    }
}
