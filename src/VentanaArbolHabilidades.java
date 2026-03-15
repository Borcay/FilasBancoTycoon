import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Ventana del árbol de habilidades (se abre desde el botón de billetes en el header).
 */
public class VentanaArbolHabilidades extends JDialog {

    private static final Color C_BG      = new Color(12, 18, 40);
    private static final Color C_ACCENT  = new Color(255, 210, 50);
    private static final Color C_TEXT    = new Color(220, 230, 255);
    private static final Color C_LOCKED  = new Color(50, 55, 80);
    private static final Color C_DONE    = new Color(40, 160, 90);

    private final PrestigioManager prestige;
    private final Runnable onCompra;

    public VentanaArbolHabilidades(JFrame parent, PrestigioManager prestige, Runnable onCompra) {
        super(parent, "Arbol de Habilidades", false); // no-modal para no bloquear el juego
        this.prestige = prestige;
        this.onCompra = onCompra;
        setSize(620, 400);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(C_BG);
        setLayout(new BorderLayout());
        construir();
    }

    private void construir() {
        getContentPane().removeAll();

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(20, 30, 65));
        header.setBorder(new EmptyBorder(12, 18, 12, 18));

        JLabel tit = new JLabel("Arbol de Habilidades");
        tit.setFont(new Font("Segoe UI", Font.BOLD, 18));
        tit.setForeground(C_ACCENT);
        header.add(tit, BorderLayout.WEST);

        JLabel lblBilletes = new JLabel("Billetes: " + prestige.getBilletes() + " B");
        lblBilletes.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblBilletes.setForeground(new Color(255, 200, 80));
        header.add(lblBilletes, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // Nodos
        JPanel grid = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 14));
        grid.setBackground(C_BG);
        grid.setBorder(new EmptyBorder(16, 16, 16, 16));

        for (PrestigioManager.Habilidad h : PrestigioManager.Habilidad.values()) {
            grid.add(crearNodo(h));
        }

        add(new JScrollPane(grid,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {{
                setBorder(null);
                getViewport().setBackground(C_BG);
                setBackground(C_BG);
            }}, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private JPanel crearNodo(PrestigioManager.Habilidad h) {
        boolean done   = prestige.estaDesbloqueada(h);
        boolean canBuy = !done && prestige.getBilletes() >= h.costoBilletes;

        Color bg     = done   ? new Color(20, 60, 35)
                     : canBuy ? new Color(20, 30, 65)
                     :          new Color(20, 22, 38);
        Color border = done   ? C_DONE
                     : canBuy ? new Color(120, 150, 255)
                     :          new Color(50, 55, 80);

        JPanel node = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(border);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 14, 14);
                g2.setStroke(new BasicStroke(1f));
            }
        };
        node.setLayout(new BoxLayout(node, BoxLayout.Y_AXIS));
        node.setOpaque(false);
        node.setPreferredSize(new Dimension(170, 145));
        node.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Nombre
        JLabel lNom = new JLabel("<html><body style='width:140px'>" + h.nombre + "</body></html>");
        lNom.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lNom.setForeground(done ? C_DONE : canBuy ? new Color(180, 200, 255) : new Color(100, 110, 140));
        lNom.setAlignmentX(Component.LEFT_ALIGNMENT);
        node.add(lNom);
        node.add(Box.createVerticalStrut(5));

        // Descripción
        JLabel lDesc = new JLabel("<html><body style='width:140px'>" + h.descripcion + "</body></html>");
        lDesc.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lDesc.setForeground(done ? new Color(140, 200, 160) : new Color(150, 160, 190));
        lDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        node.add(lDesc);
        node.add(Box.createVerticalGlue());

        // Botón / estado
        if (done) {
            JLabel lDone = new JLabel("DESBLOQUEADO");
            lDone.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lDone.setForeground(C_DONE);
            lDone.setAlignmentX(Component.LEFT_ALIGNMENT);
            node.add(lDone);
        } else {
            JButton btn = new JButton(h.costoBilletes + " B");
            btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btn.setForeground(Color.WHITE);
            btn.setBackground(canBuy ? new Color(80, 50, 160) : C_LOCKED);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setEnabled(canBuy);
            btn.setCursor(canBuy ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.addActionListener(e -> {
                if (prestige.desbloquear(h)) {
                    if (onCompra != null) onCompra.run();
                    construir(); // reconstruir para reflejar cambios
                }
            });
            node.add(btn);
        }

        return node;
    }
}
