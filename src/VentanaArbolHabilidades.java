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
            { 60, 152},  // 0 Descuento mejoras   - izquierda (nodo raíz visible)
            {400,  30},  // 1 Bonus monedas        - arriba der
            {400, 122},  // 2 Mejoras iniciales    - centro-arriba der
            {400, 214},  // 3 Menos ladrones       - centro-abajo der
            {400, 306},  // 4 Click manual         - abajo der
        };

        ArbolPanel() {
            setBackground(C_BG);
            setPreferredSize(new Dimension(680, 390));
            setLayout(null); // null layout para el tooltip flotante

        // Labels persistentes del tooltip (se actualiza el texto, no se recrea)
        JLabel tooltipNombre = new JLabel();
        JLabel tooltipDesc   = new JLabel();
        JLabel tooltipEstado = new JLabel();

        // Tooltip flotante
        JPanel tooltip = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(15, 20, 50, 230));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(C_PURPLE);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 10, 10);
                g2.setStroke(new BasicStroke(1f));
            }
        };
        tooltip.setOpaque(false);
        tooltip.setLayout(new BoxLayout(tooltip, BoxLayout.Y_AXIS));
        tooltip.setBorder(new EmptyBorder(8, 10, 8, 10));

        tooltipNombre.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tooltipNombre.setAlignmentX(Component.LEFT_ALIGNMENT);
        tooltipDesc.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tooltipDesc.setForeground(C_TEXT);
        tooltipDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        tooltipEstado.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        tooltipEstado.setAlignmentX(Component.LEFT_ALIGNMENT);

        tooltip.add(tooltipNombre);
        tooltip.add(Box.createVerticalStrut(4));
        tooltip.add(tooltipDesc);
        tooltip.add(Box.createVerticalStrut(4));
        tooltip.add(tooltipEstado);
        tooltip.setVisible(false);
        add(tooltip);

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    PrestigioManager.Habilidad[] habs = PrestigioManager.Habilidad.values();
                    for (int i = 0; i < habs.length; i++) {
                        int nx = POSICIONES[i][0], ny = POSICIONES[i][1];
                        if (e.getX() >= nx && e.getX() <= nx+NW && e.getY() >= ny && e.getY() <= ny+NH) {
                            PrestigioManager.Habilidad h = habs[i];
                            // Actualizar texto del tooltip
                            tooltipNombre.setText(h.nombre);
                            tooltipNombre.setForeground(prestige.estaDesbloqueada(h) ? C_DONE : C_PURPLE);
                            tooltipDesc.setText("<html><body style='width:190px'>" + h.descripcion + "</body></html>");
                            String estado = prestige.estaDesbloqueada(h) ? "Ya desbloqueada"
                                : prestige.getBilletes() >= h.costoBilletes
                                    ? "Costo: " + h.costoBilletes + " B — click para comprar"
                                    : "Necesitas " + h.costoBilletes + " B (tienes " + prestige.getBilletes() + ")";
                            tooltipEstado.setText(estado);
                            tooltipEstado.setForeground(prestige.estaDesbloqueada(h) ? C_DONE
                                : prestige.getBilletes() >= h.costoBilletes ? C_ACCENT
                                : new Color(180, 80, 80));
                            // Posicionar
                            int tx = nx + NW + 8;
                            int ty = ny;
                            if (tx + 220 > getWidth()) tx = nx - 228;
                            if (ty + 100 > getHeight()) ty = getHeight() - 100;
                            tooltip.setBounds(tx, ty, 220, 95);
                            tooltip.setVisible(true);
                            repaint();
                            return;
                        }
                    }
                    tooltip.setVisible(false);
                    repaint();
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseExited(MouseEvent e) { tooltip.setVisible(false); repaint(); }
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
            if (prestige.getBilletes() < h.costoBilletes) return; // sin billetes, ignorar silenciosamente
            if (prestige.desbloquear(h)) {
                if (onCompra != null) onCompra.run();
                construir();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            PrestigioManager.Habilidad[] habs = PrestigioManager.Habilidad.values();

            // Líneas: Descuento (índice 0) → las otras 4 habilidades
            int dcx = POSICIONES[0][0] + NW;
            int dcy = POSICIONES[0][1] + NH/2;
            for (int i = 1; i < habs.length; i++) {
                boolean doneI = prestige.estaDesbloqueada(habs[i]);
                boolean done0 = prestige.estaDesbloqueada(habs[0]);
                int nx = POSICIONES[i][0];
                int ny = POSICIONES[i][1] + NH/2;
                // Línea activa si Descuento está desbloqueado Y la habilidad destino también
                Color lineColor = (done0 && doneI) ? C_DONE
                                : done0 ? new Color(80, 90, 160)
                                : new Color(40, 45, 75);
                g2.setColor(lineColor);
                g2.setStroke(new BasicStroke((done0 && doneI) ? 2.5f : 1.5f,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int mx = (dcx + nx) / 2;
                QuadCurve2D curve = new QuadCurve2D.Float(dcx, dcy, mx, ny, nx, ny);
                g2.draw(curve);
            }
            g2.setStroke(new BasicStroke(1f));

            // Dibujar todos los nodos
            for (int i = 0; i < habs.length; i++) {
                dibujarNodo(g2, habs[i], POSICIONES[i][0], POSICIONES[i][1]);
            }
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
