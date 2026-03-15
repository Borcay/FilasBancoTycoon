import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Ventana que muestra todos los bancos corriendo en paralelo en tiempo real.
 * Solo visible tras el primer prestige.
 */
public class VentanaBancos extends JDialog {

    private static final Color C_BG     = new Color(14, 20, 45);
    private static final Color C_CARD   = new Color(22, 32, 68);
    private static final Color C_ACCENT = new Color(255, 210, 50);
    private static final Color C_TEXT   = new Color(200, 215, 255);
    private static final Color C_GREEN  = new Color(40, 180, 90);
    private static final Color C_GOLD   = new Color(220, 160, 10);

    private final List<SimulacionBanco>  sims;
    private final List<Economia>         ecos;
    private final PrestigioManager       prestige;
    private final Runnable               onCompraBanco;

    private JPanel panelBancos;
    private javax.swing.Timer refreshTimer;

    public VentanaBancos(JFrame parent, List<SimulacionBanco> sims,
                         List<Economia> ecos, PrestigioManager prestige,
                         Runnable onCompraBanco) {
        super(parent, "Mis Bancos", false); // non-modal
        this.sims          = sims;
        this.ecos          = ecos;
        this.prestige      = prestige;
        this.onCompraBanco = onCompraBanco;

        setSize(700, 450);
        setLocationRelativeTo(parent);
        setResizable(true);
        getContentPane().setBackground(C_BG);
        setLayout(new BorderLayout());

        construirUI();
        iniciarRefresh();

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (refreshTimer != null) refreshTimer.stop();
            }
        });
    }

    private void construirUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(18, 28, 60));
        header.setBorder(new EmptyBorder(12, 18, 12, 18));

        JLabel tit = new JLabel("Mis Bancos");
        tit.setFont(new Font("Segoe UI", Font.BOLD, 18));
        tit.setForeground(C_ACCENT);
        header.add(tit, BorderLayout.WEST);

        JLabel lblB = new JLabel("Billetes: " + prestige.getBilletes() + " B");
        lblB.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblB.setForeground(new Color(255, 200, 80));
        header.add(lblB, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // Panel de tarjetas de bancos
        panelBancos = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 14));
        panelBancos.setBackground(C_BG);

        JScrollPane scroll = new JScrollPane(panelBancos,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(C_BG);
        add(scroll, BorderLayout.CENTER);

        // Footer: botón comprar banco
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
        footer.setBackground(new Color(18, 28, 60));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(50, 70, 130)));

        JButton btnComprar = new JButton("Comprar Banco  (" + prestige.costoBanco() + " B)");
        btnComprar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnComprar.setForeground(Color.WHITE);
        btnComprar.setBackground(new Color(80, 50, 160));
        btnComprar.setBorderPainted(false);
        btnComprar.setFocusPainted(false);
        btnComprar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnComprar.addActionListener(e -> {
            if (prestige.comprarBanco()) {
                if (onCompraBanco != null) onCompraBanco.run();
                refrescar(); // actualizar vista
                btnComprar.setText("Comprar Banco  (" + prestige.costoBanco() + " B)");
            } else {
                JOptionPane.showMessageDialog(this,
                    "No tienes suficientes billetes.\nNecesitas " + prestige.costoBanco() + " B.",
                    "Sin billetes", JOptionPane.WARNING_MESSAGE);
            }
        });

        JLabel hint = new JLabel("Cada banco corre en paralelo y genera monedas de forma independiente.");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        hint.setForeground(new Color(120, 140, 180));

        footer.add(btnComprar);
        footer.add(hint);
        add(footer, BorderLayout.SOUTH);

        refrescar();
    }

    private void refrescar() {
        panelBancos.removeAll();
        for (int i = 0; i < sims.size(); i++) {
            panelBancos.add(crearTarjetaBanco(i, sims.get(i), ecos.get(i)));
        }
        panelBancos.revalidate();
        panelBancos.repaint();
    }

    private JPanel crearTarjetaBanco(int idx, SimulacionBanco sim, Economia eco) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                Color bc = sim.isTerminado() ? new Color(80, 200, 120) : new Color(60, 90, 160);
                g2.setColor(bc);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 14, 14);
                g2.setStroke(new BasicStroke(1f));
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(200, 220));
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        String nombreBanco = "Banco " + (idx + 1);

        JLabel lNom = new JLabel(nombreBanco);
        lNom.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lNom.setForeground(C_ACCENT);
        lNom.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lNom);
        card.add(Box.createVerticalStrut(8));

        // Progreso de clientes
        int atendidos = sim.getClientesAtendidosTotal();
        int meta      = sim.getMetaClientes();
        JLabel lProg = new JLabel("Clientes: " + atendidos + " / " + meta);
        lProg.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lProg.setForeground(C_TEXT);
        lProg.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lProg);

        // Barra de progreso
        JPanel barra = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(40, 50, 90));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                double pct = Math.min(1.0, (double) atendidos / meta);
                g2.setColor(sim.isTerminado() ? C_GREEN : new Color(80, 150, 255));
                g2.fillRoundRect(0, 0, (int)(getWidth() * pct), getHeight(), 6, 6);
            }
        };
        barra.setPreferredSize(new Dimension(170, 8));
        barra.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        barra.setOpaque(false);
        barra.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(barra);
        card.add(Box.createVerticalStrut(8));

        // Monedas del banco
        JLabel lMon = new JLabel("Monedas: $" + eco.getMonedas());
        lMon.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lMon.setForeground(C_GOLD);
        lMon.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lMon);

        JLabel lGan = new JLabel("Ganado hoy: $" + eco.getGananciasHoy());
        lGan.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lGan.setForeground(new Color(150, 180, 150));
        lGan.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lGan);

        card.add(Box.createVerticalStrut(6));

        // Cajeros activos
        long abiertos = sim.getCajeros().stream().filter(c -> c.isAbierto()).count();
        JLabel lCaj = new JLabel("Cajeros: " + abiertos + " / " + sim.getCajeros().size() + " abiertos");
        lCaj.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lCaj.setForeground(C_TEXT);
        lCaj.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lCaj);

        card.add(Box.createVerticalStrut(6));

        // Día actual
        JLabel lDia = new JLabel("Dia " + eco.getDia());
        lDia.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lDia.setForeground(new Color(130, 150, 200));
        lDia.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lDia);

        card.add(Box.createVerticalGlue());

        // Estado
        String estadoTxt = sim.isTerminado() ? "LISTO PARA PRESTIGE" : "En progreso...";
        Color estadoColor = sim.isTerminado() ? C_GREEN : new Color(100, 140, 220);
        JLabel lEst = new JLabel(estadoTxt);
        lEst.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lEst.setForeground(estadoColor);
        lEst.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lEst);

        return card;
    }

    private void iniciarRefresh() {
        refreshTimer = new javax.swing.Timer(1000, e -> refrescar());
        refreshTimer.start();
    }

    public void detenerRefresh() {
        if (refreshTimer != null) refreshTimer.stop();
    }
}
