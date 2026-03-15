import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.QuadCurve2D;

/**
 * Árbol de habilidades visual con nodo central y ramas hacia cada habilidad.
 */
public class VentanaArbolHabilidades extends JDialog {

    private static final Color C_BG       = new Color(10, 14, 35);
    private static final Color C_NODO_BG  = new Color(20, 28, 65);
    private static final Color C_DONE     = new Color(40, 160, 90);
    private static final Color C_LOCKED   = new Color(35, 38, 60);
    private static final Color C_CANBY    = new Color(25, 40, 90);
    private static final Color C_TEXT     = new Color(200, 215, 255);
    private static final Color C_ACCENT   = new Color(255, 210, 50);
    private static final Color C_PURPLE   = new Color(160, 100, 255);

    private final PrestigioManager prestige;
    private final Runnable onCompra;

    public VentanaArbolHabilidades(JFrame parent, PrestigioManager prestige, Runnable onCompra) {
        super(parent, "Arbol de Habilidades", false);
        this.prestige = prestige;
        this.onCompra = onCompra;
        setSize(680, 460);
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
        header.setBackground(new Color(16, 22, 52));
        header.setBorder(new EmptyBorder(10, 18, 10, 18));
        JLabel tit = new JLabel("Arbol de Habilidades");
        tit.setFont(new Font("Segoe UI", Font.BOLD, 17));
        tit.setForeground(C_ACCENT);
        header.add(tit, BorderLayout.WEST);
        JLabel lblB = new JLabel("Billetes disponibles: " + prestige.getBilletes() + " B");
        lblB.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblB.setForeground(C_PURPLE);
        header.add(lblB, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Panel principal con el árbol dibujado
        ArbolPanel arbol = new ArbolPanel();
        add(arbol, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    // ── Panel que dibuja el árbol ──────────────────────────────────────────
    class ArbolPanel extends JPanel {

        private static final int NW = 140; // nodo ancho
        private static final int NH = 76;  // nodo alto

        // Árbol: Nodo raíz (izq) → Descuento (centro) → 4 habilidades (der)
        // Orden en Habilidad.values(): DESCUENTO(0), BONUS_MONEDAS(1), MEJORAS_INICIALES(2), MENOS_LADRONES(3), CLICK_MANUAL(4)
        // Descuento va al centro como nodo intermedio
        // Las otras 4 salen del Descuento
        private final int[][] POSICIONES = {
            {255, 162},  // 0 Descuento mejoras   - CENTRO (nodo intermedio)
            {440,  40},  // 1 Bonus monedas        - arriba der
            {440, 130},  // 2 Mejoras iniciales    - centro-arriba der
            {440, 220},  // 3 Menos ladrones       - centro-abajo der
            {440, 310},  // 4 Click manual         - abajo der
        };

        // Nodo raíz a la izquierda
        private final int RX = 50, RY = 162;

        ArbolPanel() {
            setBackground(C_BG);
            setPreferredSize(new Dimension(680, 390));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    PrestigioManager.Habilidad[] habs = PrestigioManager.Habilidad.values();
                    for (int i = 0; i < habs.length; i++) {
                        int nx = POSICIONES[i][0], ny = POSICIONES[i][1];
                        if (e.getX() >= nx && e.getX() <= nx+NW &&
                            e.getY() >= ny && e.getY() <= ny+NH) {
                            onClickNodo(habs[i]);
                            return;
                        }
                    }
                }
            });
        }

        private void onClickNodo(PrestigioManager.Habilidad h) {
            if (prestige.estaDesbloqueada(h)) return;
            if (prestige.getBilletes() < h.costoBilletes) {
                JOptionPane.showMessageDialog(VentanaArbolHabilidades.this,
                    "Necesitas " + h.costoBilletes + " billete(s). Tienes " + prestige.getBilletes() + ".",
                    "Sin billetes", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int ok = JOptionPane.showConfirmDialog(VentanaArbolHabilidades.this,
                "¿Desbloquear \"" + h.nombre + "\" por " + h.costoBilletes + " billete(s)?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION && prestige.desbloquear(h)) {
                if (onCompra != null) onCompra.run();
                construir(); // rebuild to reflect new state
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            PrestigioManager.Habilidad[] habs = PrestigioManager.Habilidad.values();

            // Dibujar línea raíz → Descuento (índice 0)
            {
                boolean done0 = prestige.estaDesbloqueada(habs[0]);
                int dcx = POSICIONES[0][0] + NW/2;
                int dcy = POSICIONES[0][1] + NH/2;
                g2.setColor(done0 ? C_DONE : new Color(50, 60, 100));
                g2.setStroke(new BasicStroke(done0 ? 2.5f : 1.5f,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(RX + NW, RY + NH/2, dcx, dcy);
            }
            g2.setStroke(new BasicStroke(1f));

            // Dibujar líneas Descuento → habilidades 1..4
            int dcx = POSICIONES[0][0] + NW;
            int dcy = POSICIONES[0][1] + NH/2;
            for (int i = 1; i < habs.length; i++) {
                boolean doneI = prestige.estaDesbloqueada(habs[i]);
                int nx = POSICIONES[i][0];
                int ny = POSICIONES[i][1] + NH/2;
                Color lineColor = doneI ? C_DONE : new Color(50, 60, 100);
                g2.setColor(lineColor);
                g2.setStroke(new BasicStroke(doneI ? 2.5f : 1.5f,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int mx = (dcx + nx) / 2;
                QuadCurve2D curve = new QuadCurve2D.Float(dcx, dcy, mx, ny, nx, ny);
                g2.draw(curve);
            }
            g2.setStroke(new BasicStroke(1f));

            // Dibujar nodo raíz
            dibujarNodoRaiz(g2);

            // Dibujar nodos de habilidades
            for (int i = 0; i < habs.length; i++) {
                dibujarNodo(g2, habs[i], POSICIONES[i][0], POSICIONES[i][1]);
            }
        }

        private void dibujarNodoRaiz(Graphics2D g2) {
            int x = RX, y = RY;
            g2.setColor(new Color(30, 50, 110));
            g2.fillRoundRect(x, y, NW, NH, 14, 14);
            g2.setColor(C_ACCENT);
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRoundRect(x, y, NW, NH, 14, 14);
            g2.setStroke(new BasicStroke(1f));
            // Estrella / icono
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.setColor(C_ACCENT);
            String t1 = "Arbol de";
            String t2 = "Habilidades";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(t1, x + (NW - fm.stringWidth(t1))/2, y + NH/2 - 4);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            fm = g2.getFontMetrics();
            g2.setColor(new Color(200, 180, 100));
            g2.drawString(t2, x + (NW - fm.stringWidth(t2))/2, y + NH/2 + 12);
        }

        private void dibujarNodo(Graphics2D g2, PrestigioManager.Habilidad h, int x, int y) {
            boolean done   = prestige.estaDesbloqueada(h);
            boolean canBuy = !done && prestige.getBilletes() >= h.costoBilletes;

            Color bg     = done ? new Color(18, 55, 35) : canBuy ? C_CANBY : C_LOCKED;
            Color border  = done ? C_DONE : canBuy ? C_PURPLE : new Color(50, 55, 85);

            // Sombra
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillRoundRect(x+3, y+3, NW, NH, 12, 12);

            // Fondo
            g2.setColor(bg);
            g2.fillRoundRect(x, y, NW, NH, 12, 12);

            // Borde
            g2.setColor(border);
            g2.setStroke(new BasicStroke(done ? 2.5f : canBuy ? 2f : 1.2f));
            g2.drawRoundRect(x, y, NW, NH, 12, 12);
            g2.setStroke(new BasicStroke(1f));

            // Nombre
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.setColor(done ? C_DONE : canBuy ? new Color(180, 200, 255) : new Color(90, 95, 130));
            FontMetrics fm = g2.getFontMetrics();
            // Wrap nombre si es largo
            String nom = h.nombre;
            int ty = y + 16;
            if (fm.stringWidth(nom) > NW - 12) {
                String[] words = nom.split(" ");
                String line1 = "", line2 = "";
                for (String w : words) {
                    if (fm.stringWidth(line1 + " " + w) < NW - 12) line1 += (line1.isEmpty() ? "" : " ") + w;
                    else line2 += (line2.isEmpty() ? "" : " ") + w;
                }
                g2.drawString(line1, x + (NW - fm.stringWidth(line1))/2, ty);
                if (!line2.isEmpty()) {
                    ty += 14;
                    g2.drawString(line2, x + (NW - fm.stringWidth(line2))/2, ty);
                }
            } else {
                g2.drawString(nom, x + (NW - fm.stringWidth(nom))/2, ty);
            }

            // Costo o estado
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            fm = g2.getFontMetrics();
            String estado = done ? "DESBLOQUEADO" : h.costoBilletes + " billete(s)";
            Color estadoColor = done ? C_DONE : canBuy ? C_ACCENT : new Color(80, 85, 110);
            g2.setColor(estadoColor);
            g2.drawString(estado, x + (NW - fm.stringWidth(estado))/2, y + NH - 10);

            // Cursor hint si puede comprar
            if (canBuy) {
                g2.setColor(new Color(160, 100, 255, 120));
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{3f, 3f}, 0f));
                g2.drawRoundRect(x+2, y+2, NW-4, NH-4, 10, 10);
                g2.setStroke(new BasicStroke(1f));
            }
        }
    }
}
