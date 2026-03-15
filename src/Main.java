import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class Main {

    // Listas globales de bancos (crece con cada compra)
    private static final List<SimulacionBanco> todasSims = new ArrayList<>();
    private static final List<Economia>        todasEcos = new ArrayList<>();
    private static PrestigioManager            prestige  = new PrestigioManager();
    private static InterfazGrafica             guiActual;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> iniciarNuevoBanco(false));
    }

    /**
     * Inicia (o reinicia tras prestige) el banco principal.
     * @param esPrestigio true = reinicio tras prestige, false = primer arranque
     */
    private static void iniciarNuevoBanco(boolean esPrestigio) {
        Economia eco = new Economia();
        SimulacionBanco sim = new SimulacionBanco(eco);
        sim.setPrestigio(prestige);

        // En el primer arranque llenamos la lista; en prestige reemplazamos el índice 0
        // pero los demás bancos comprados siguen corriendo
        if (!esPrestigio || todasSims.isEmpty()) {
            todasSims.add(sim);
            todasEcos.add(eco);
        } else {
            // Reemplazar el primer banco (el que se acaba de prestigiar)
            todasSims.set(0, sim);
            todasEcos.set(0, eco);
        }

        if (guiActual != null) {
            guiActual.dispose();
        }

        guiActual = new InterfazGrafica(sim, eco);
        sim.setGui(guiActual);

        // Callback: cuando el jugador hace prestige
        guiActual.setOnPrestigio(() -> SwingUtilities.invokeLater(() -> iniciarNuevoBanco(true)));

        // Callback: abrir ventana de bancos
        guiActual.setOnAbrirBancos(() -> SwingUtilities.invokeLater(() ->
            guiActual.mostrarVentanaBancos(
                new ArrayList<>(todasSims),
                new ArrayList<>(todasEcos),
                prestige,
                () -> agregarNuevoBanco())));

        // Si ya había hecho al menos 1 prestige, activar UI de prestige desde el inicio
        if (prestige.getPrestigios() > 0) {
            guiActual.activarUIPrestigioPublico();
        }

        guiActual.setVisible(true);
        sim.iniciar();

        // Si hay bancos adicionales comprados, asegurarse de que sigan corriendo
        // (los bancos extra no se reinician, solo el banco 0)
        for (int i = 1; i < todasSims.size(); i++) {
            if (todasSims.get(i).isTerminado()) {
                // banco terminado, dejarlo así (muestra stats en VentanaBancos)
            }
        }
    }

    /** Llamado por VentanaBancos cuando se compra un nuevo banco */
    public static void agregarNuevoBanco() {
        Economia eco = new Economia();
        SimulacionBanco sim = new SimulacionBanco(eco);
        sim.setPrestigio(prestige);
        todasSims.add(sim);
        todasEcos.add(eco);
        // El nuevo banco corre en paralelo, sin GUI propia (se ve en VentanaBancos)
        sim.iniciar();
    }
}
