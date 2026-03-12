import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SimulacionBanco simulacion = new SimulacionBanco();
            InterfazGrafica gui = new InterfazGrafica(simulacion);
            simulacion.setGui(gui);
            gui.setVisible(true);
            simulacion.iniciar();
        });
    }
}
