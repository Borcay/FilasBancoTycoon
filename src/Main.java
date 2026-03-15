import javax.swing.*;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final List<SimulacionBanco> todasSims = new ArrayList<>();
    private static final List<Economia>        todasEcos = new ArrayList<>();
    private static final PrestigioManager      prestige  = new PrestigioManager();
    private static InterfazGrafica             guiActual;

    public static void main(String[] args) {
        // PaseBatalla global sin eco — se conecta al eco activo en setPrestigio()
        prestige.setPaseBatallaGlobal(new PaseBatalla(null));
        SwingUtilities.invokeLater(() -> iniciarNuevoBanco(false));
    }

    private static void iniciarNuevoBanco(boolean esPrestigio) {
        Economia eco = new Economia();
        SimulacionBanco sim = new SimulacionBanco(eco);
        sim.setPrestigio(prestige);

        if (!esPrestigio || todasSims.isEmpty()) {
            todasSims.add(sim);
            todasEcos.add(eco);
        } else {
            // Detener el banco viejo antes de reemplazarlo
            SimulacionBanco bancoViejo = todasSims.get(0);
            bancoViejo.detenerTodo();
            todasSims.set(0, sim);
            todasEcos.set(0, eco);
        }

        if (guiActual != null) guiActual.dispose();

        guiActual = new InterfazGrafica(sim, eco);
        sim.setGui(guiActual);

        // Callback: prestige realizado
        guiActual.setOnPrestigio(() -> SwingUtilities.invokeLater(() -> iniciarNuevoBanco(true)));

        // Callback: cambiar vista a un banco distinto
        guiActual.setOnCambiarBanco((InterfazGrafica.BancoIndexConsumer) idx -> SwingUtilities.invokeLater(() -> {
            if (idx < 0 || idx >= todasSims.size()) return;
            SimulacionBanco s = todasSims.get(idx);
            Economia e = todasEcos.get(idx);
            guiActual.cambiarVistaBanco(s, e);
            guiActual.refrescarPanelLateral(new ArrayList<>(todasSims));
        }));

        guiActual.setOnObtenerIndiceBanco((InterfazGrafica.BancoIndexFunction) s -> todasSims.indexOf(s));

        // Callback: refrescar cubos (tras prestige)
        guiActual.setOnRefrescarCubos(() -> SwingUtilities.invokeLater(() ->
            guiActual.refrescarPanelLateral(new ArrayList<>(todasSims))));

        // Callback: abrir ventana de bancos (ya no se usa, panel es lateral)
        guiActual.setOnAbrirBancos(() -> {});

        // Callback: comprar nuevo banco desde panel lateral
        guiActual.setOnCompraBancoLateral(() -> SwingUtilities.invokeLater(() -> {
            if (prestige.comprarBanco()) {
                agregarNuevoBanco();
                guiActual.mostrarToast("Nuevo banco comprado!", new Color(180, 120, 255));
            } else {
                guiActual.mostrarToast("Sin billetes suficientes (" + prestige.costoBanco() + " B necesarios)", new Color(180, 60, 60));
            }
        }));

        // Si ya hizo al menos 1 prestige, mostrar UI de prestige y panel lateral
        if (prestige.getPrestigios() > 0) {
            guiActual.activarUIPrestigioPublico();
            guiActual.refrescarPanelLateral(new ArrayList<>(todasSims));
        }

        guiActual.setVisible(true);
        sim.iniciar();
    }

    /** Compra un nuevo banco y lo arranca en paralelo */
    public static void agregarNuevoBanco() {
        Economia eco = new Economia();
        SimulacionBanco sim = new SimulacionBanco(eco);
        sim.setPrestigio(prestige);
        todasSims.add(sim);
        todasEcos.add(eco);
        sim.iniciar();
        if (guiActual != null)
            guiActual.refrescarPanelLateral(new ArrayList<>(todasSims));
    }
}
