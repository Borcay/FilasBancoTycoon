import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Economia eco = new Economia();
            SimulacionBanco sim = new SimulacionBanco(eco);
            InterfazGrafica gui = new InterfazGrafica(sim, eco);
            sim.setGui(gui);
            gui.setVisible(true);
            sim.iniciar();
        });
    }
}
