import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Diálogo que aparece al llegar a 200 clientes ofreciendo Prestige o continuar.
 */
public class VentanaPrestigio extends JDialog {

    private static final Color C_BG       = new Color(12, 18, 40);
    private static final Color C_ACCENT   = new Color(255, 210, 50);
    private static final Color C_ACCENT2  = new Color(180, 120, 255);
    private static final Color C_TEXT     = new Color(220, 230, 255);
    private static final Color C_BTN_PRES = new Color(180, 120, 255);
    private static final Color C_BTN_CONT = new Color(50, 80, 160);

    public enum Resultado { PRESTIGE, CONTINUAR }

    private Resultado resultado = Resultado.CONTINUAR;

    public VentanaPrestigio(JFrame parent, PrestigioManager prestige, int totalClientes) {
        super(parent, "200 Clientes Atendidos!", true);
        setSize(500, 360);
        setLocationRelativeTo(parent);
        setUndecorated(false);
        setResizable(false);
        getContentPane().setBackground(C_BG);
        setLayout(new BorderLayout());

        add(crearContenido(prestige, totalClientes));
    }

    private JPanel crearContenido(PrestigioManager prestige, int totalClientes) {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(C_BG);
        p.setBorder(new EmptyBorder(24, 28, 20, 28));

        // ── Título ──
        JLabel tit = new JLabel("¡200 Clientes Atendidos!", SwingConstants.CENTER);
        tit.setFont(new Font("Segoe UI", Font.BOLD, 22));
        tit.setForeground(C_ACCENT);
        tit.setBorder(new EmptyBorder(0, 0, 6, 0));

        JLabel sub = new JLabel("Puedes hacer Prestige o continuar jugando.", SwingConstants.CENTER);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(C_TEXT);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(C_BG);
        topPanel.add(tit);
        topPanel.add(sub);
        p.add(topPanel, BorderLayout.NORTH);

        // ── Dos tarjetas lado a lado ──
        JPanel cards = new JPanel(new GridLayout(1, 2, 16, 0));
        cards.setBackground(C_BG);
        cards.setBorder(new EmptyBorder(18, 0, 18, 0));

        // Tarjeta Prestige
        JPanel cardPres = crearTarjeta(
            "PRESTIGE",
            new Color(50, 20, 80),
            new Color(180, 120, 255),
            new String[]{
                "+" + prestige.billetesDelPrestige() + " billete(s)",
                "Reinicia monedas y mejoras",
                "El Pase de Batalla se conserva",
                "Desbloquea el arbol de habilidades",
                prestige.getNumeroBancos() == 1 ? "Nuevo banco disponible para comprar" : ""
            }
        );

        // Tarjeta Continuar
        JPanel cardCont = crearTarjeta(
            "CONTINUAR",
            new Color(15, 25, 55),
            new Color(100, 150, 255),
            new String[]{
                "Mantén tus monedas ($" + "actuales" + ")",
                "Mantén todas tus mejoras",
                "Sigue acumulando clientes",
                "Sin recompensa de prestige",
                ""
            }
        );

        cards.add(cardPres);
        cards.add(cardCont);
        p.add(cards, BorderLayout.CENTER);

        // ── Botones ──
        JPanel bots = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        bots.setBackground(C_BG);

        JButton btnPres = mkBtn("Hacer Prestige", C_BTN_PRES);
        btnPres.addActionListener(e -> {
            resultado = Resultado.PRESTIGE;
            dispose();
        });

        JButton btnCont = mkBtn("Continuar", C_BTN_CONT);
        btnCont.addActionListener(e -> {
            resultado = Resultado.CONTINUAR;
            dispose();
        });

        bots.add(btnPres);
        bots.add(btnCont);
        p.add(bots, BorderLayout.SOUTH);

        return p;
    }

    private JPanel crearTarjeta(String titulo, Color bgColor, Color accentColor, String[] items) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(accentColor);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 14, 14);
                g2.setStroke(new BasicStroke(1f));
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel tit = new JLabel(titulo);
        tit.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tit.setForeground(accentColor);
        tit.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(tit);
        card.add(Box.createVerticalStrut(8));

        for (String item : items) {
            if (item == null || item.isBlank()) continue;
            JLabel l = new JLabel("• " + item);
            l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            l.setForeground(C_TEXT);
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(l);
            card.add(Box.createVerticalStrut(3));
        }
        return card;
    }

    private JButton mkBtn(String txt, Color bg) {
        JButton b = new JButton(txt);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(160, 36));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public Resultado getResultado() { return resultado; }
}
