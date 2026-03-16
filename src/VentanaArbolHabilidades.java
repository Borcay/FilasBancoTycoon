import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.QuadCurve2D;
import java.util.Random;

public class VentanaArbolHabilidades extends JDialog {

    private static final Color C_BG     = new Color(5, 8, 22);
    private static final Color C_DONE   = new Color(40, 160, 90);
    private static final Color C_LOCKED = new Color(20, 22, 38);
    private static final Color C_CANBY  = new Color(20, 35, 80);
    private static final Color C_TEXT   = new Color(200, 215, 255);
    private static final Color C_ACCENT = new Color(255, 210, 50);
    private static final Color C_PURPLE = new Color(160, 100, 255);

    private final PrestigioManager prestige;
    private final Runnable onCompra;
    private JLabel lblBilletes;

    public VentanaArbolHabilidades(JFrame parent, PrestigioManager prestige, Runnable onCompra) {
        super(parent, "Arbol de Habilidades", false);
        this.prestige = prestige;
        this.onCompra = onCompra;
        setSize(700, 520);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(C_BG);
        setLayout(new BorderLayout());
        construir();
    }

    private void construir() {
        getContentPane().removeAll();
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(10, 15, 40));
        header.setBorder(new EmptyBorder(10, 18, 10, 18));
        JLabel tit = new JLabel("Arbol de Habilidades");
        tit.setFont(new Font("Segoe UI", Font.BOLD, 17));
        tit.setForeground(C_ACCENT);
        header.add(tit, BorderLayout.WEST);
        lblBilletes = new JLabel("Billetes: " + prestige.getBilletes() + " B");
        lblBilletes.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblBilletes.setForeground(C_PURPLE);
        header.add(lblBilletes, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        add(getArbolPanel(), BorderLayout.CENTER);
        revalidate(); repaint();
    }

    public void actualizarBilletes() {
        if (lblBilletes != null)
            lblBilletes.setText("Billetes: " + prestige.getBilletes() + " B");
    }

    public JPanel getArbolPanel() { return new ArbolPanel(); }

    class ArbolPanel extends JPanel {

        private static final int NW = 148;
        private static final int NH = 80;

        // Posiciones base (x, y) de cada nodo según el ordinal de Habilidad
        // ARBOL_DINERO=0, BONUS_MONEDAS=1, REPUTACION=2,
        // DESCUENTO_MEJORAS=3, MEJORAS_INICIALES=4, MENOS_LADRONES=5, CLICK_MANUAL=6
        private final int[][] BASE_POS = {
            { 276, 200},  // 0 ARBOL_DINERO       — raíz (centro)
            {  60, 200},  // 1 BONUS_MONEDAS       — izquierda del raíz
            { 490, 200},  // 2 REPUTACION          — derecha del raíz
            {-160,  80},  // 3 DESCUENTO_MEJORAS   — hijo izq de BONUS (arriba-izq)
            {-160, 320},  // 4 MEJORAS_INICIALES   — hijo izq de BONUS (abajo-izq)
            { 690,  80},  // 5 MENOS_LADRONES      — hijo der de REPUTACION (arriba-der)
            { 690, 320},  // 6 CLICK_MANUAL        — hijo der de REPUTACION (abajo-der)
        };

        private int panX = 0, panY = 0;
        private boolean panInitialized = false;
        private int dragStartX, dragStartY;
        private boolean dragging = false;
        private int hoveredIdx = -1;

        // Liquid
        private final double[] lqX = new double[7];
        private final double[] lqY = new double[7];
        private final double[] lqVX = new double[7];
        private final double[] lqVY = new double[7];

        // Stars
        private final int[] sX, sY;
        private final float[] sA, sD;
        private static final int NS = 130;

        // Tooltip
        private final JLabel ttNom = new JLabel();
        private final JLabel ttDesc = new JLabel();
        private final JLabel ttEst = new JLabel();
        private final JPanel tooltip;
        private final javax.swing.Timer animTimer;

        ArbolPanel() {
            setBackground(C_BG);
            setLayout(null);

            Random rnd = new Random();
            for (int i = 0; i < 7; i++) {
                lqVX[i] = (rnd.nextDouble() - 0.5) * 0.28;
                lqVY[i] = (rnd.nextDouble() - 0.5) * 0.28;
            }
            sX = new int[NS]; sY = new int[NS]; sA = new float[NS]; sD = new float[NS];
            for (int i = 0; i < NS; i++) {
                sX[i] = rnd.nextInt(1400); sY[i] = rnd.nextInt(900);
                sA[i] = rnd.nextFloat();
                sD[i] = (rnd.nextFloat() * 0.014f + 0.003f) * (rnd.nextBoolean() ? 1 : -1);
            }

            // Tooltip
            tooltip = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(10, 14, 40, 235));
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
            ttNom.setFont(new Font("Segoe UI", Font.BOLD, 12));
            ttNom.setAlignmentX(Component.LEFT_ALIGNMENT);
            ttDesc.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            ttDesc.setForeground(C_TEXT);
            ttDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
            ttEst.setFont(new Font("Segoe UI", Font.ITALIC, 10));
            ttEst.setAlignmentX(Component.LEFT_ALIGNMENT);
            tooltip.add(ttNom);
            tooltip.add(Box.createVerticalStrut(4));
            tooltip.add(ttDesc);
            tooltip.add(Box.createVerticalStrut(4));
            tooltip.add(ttEst);
            tooltip.setVisible(false);
            add(tooltip);

            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    dragStartX = e.getX() - panX;
                    dragStartY = e.getY() - panY;
                    dragging = true;
                    tooltip.setVisible(false);
                }
                @Override public void mouseReleased(MouseEvent e) {
                    int dx = Math.abs(e.getX() - panX - dragStartX);
                    int dy = Math.abs(e.getY() - panY - dragStartY);
                    if (dx < 5 && dy < 5) {
                        int idx = nodeAt(e.getX(), e.getY());
                        if (idx >= 0) onClickNodo(PrestigioManager.Habilidad.values()[idx]);
                    }
                    dragging = false;
                }
                @Override public void mouseExited(MouseEvent e) {
                    tooltip.setVisible(false); hoveredIdx = -1;
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseDragged(MouseEvent e) {
                    if (dragging) { panX = e.getX() - dragStartX; panY = e.getY() - dragStartY; repaint(); }
                }
                @Override public void mouseMoved(MouseEvent e) {
                    int idx = nodeAt(e.getX(), e.getY());
                    hoveredIdx = idx;
                    if (idx >= 0) showTooltip(idx, e.getX(), e.getY());
                    else tooltip.setVisible(false);
                }
            });

            animTimer = new javax.swing.Timer(30, e -> {
                for (int i = 0; i < 7; i++) {
                    lqX[i] += lqVX[i]; lqY[i] += lqVY[i];
                    if (Math.abs(lqX[i]) > 4) lqVX[i] *= -1;
                    if (Math.abs(lqY[i]) > 4) lqVY[i] *= -1;
                }
                for (int i = 0; i < NS; i++) {
                    sA[i] += sD[i];
                    if (sA[i] > 1f) { sA[i] = 1f; sD[i] *= -1; }
                    if (sA[i] < 0f) { sA[i] = 0f; sD[i] *= -1; }
                }
                repaint();
            });
            animTimer.start();
        }

        private int[] nodePos(int i) {
            return new int[]{ BASE_POS[i][0] + panX + (int)lqX[i],
                              BASE_POS[i][1] + panY + (int)lqY[i] };
        }

        private int nodeAt(int mx, int my) {
            PrestigioManager.Habilidad[] habs = PrestigioManager.Habilidad.values();
            for (int i = 0; i < habs.length; i++) {
                if (!prestige.esVisible(habs[i])) continue;
                int[] p = nodePos(i);
                if (mx >= p[0] && mx <= p[0]+NW && my >= p[1] && my <= p[1]+NH) return i;
            }
            return -1;
        }

        private void showTooltip(int idx, int mx, int my) {
            PrestigioManager.Habilidad h = PrestigioManager.Habilidad.values()[idx];
            ttNom.setText(h.nombre);
            ttNom.setForeground(prestige.estaDesbloqueada(h) ? C_DONE : C_PURPLE);
            ttDesc.setText("<html><body style='width:190px'>" + h.descripcion + "</body></html>");
            String est;
            if (prestige.estaDesbloqueada(h))      est = "Ya desbloqueado";
            else if (h.costoBilletes == 0)         est = "Gratis — click para desbloquear";
            else if (prestige.getBilletes() >= h.costoBilletes)
                                                    est = "Costo: " + h.costoBilletes + " B — click para comprar";
            else                                    est = "Necesitas " + h.costoBilletes + " B (tienes " + prestige.getBilletes() + ")";
            ttEst.setText(est);
            ttEst.setForeground(prestige.estaDesbloqueada(h) ? C_DONE
                : prestige.sePuedeComprar(h) ? C_ACCENT : new Color(150, 70, 70));
            int tw = 260, th = 100;
            int tx = mx - tw/2; // centrado bajo el cursor
            int ty = my + 18;
            if (tx < 4) tx = 4;
            if (tx + tw > getWidth() - 4) tx = getWidth() - tw - 4;
            if (ty + th > getHeight() - 4) ty = my - th - 10; // arriba si no cabe abajo
            tooltip.setBounds(tx, ty, tw, th);
            tooltip.setVisible(true);
        }

        private void onClickNodo(PrestigioManager.Habilidad h) {
            if (prestige.estaDesbloqueada(h)) return;
            if (!prestige.esVisible(h)) return;
            if (h.costoBilletes > 0 && prestige.getBilletes() < h.costoBilletes) return;
            if (prestige.desbloquear(h)) {
                SonidoManager.get().sonarMejoraArbol();
                actualizarBilletes();
                if (onCompra != null) onCompra.run();
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setColor(C_BG);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Centrar raíz la primera vez
            if (!panInitialized && getWidth() > 0) {
                panX = getWidth()/2  - BASE_POS[0][0] - NW/2;
                panY = getHeight()/2 - BASE_POS[0][1] - NH/2;
                panInitialized = true;
            }

            // Estrellas
            for (int i = 0; i < NS; i++) {
                int sx = ((sX[i] + panX/4) % Math.max(1,getWidth()) + getWidth()) % getWidth();
                int sy = ((sY[i] + panY/4) % Math.max(1,getHeight()) + getHeight()) % getHeight();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, sA[i]*0.55f));
                g2.setColor(new Color(200, 220, 255));
                int sz = (i%3==0)?2:1;
                g2.fillOval(sx, sy, sz, sz);
            }
            g2.setComposite(AlphaComposite.SrcOver);

            PrestigioManager.Habilidad[] habs = PrestigioManager.Habilidad.values();

            // Líneas entre nodos visibles conectados
            for (PrestigioManager.Habilidad h : habs) {
                if (h.padre == null) continue;
                if (!prestige.esVisible(h)) continue; // solo líneas a nodos visibles
                int ci = h.ordinal();
                int pi = h.padre.ordinal();
                int[] cp = nodePos(ci);
                int[] pp = nodePos(pi);
                boolean ambosOk = prestige.estaDesbloqueada(h) && prestige.estaDesbloqueada(h.padre);
                Color lc = ambosOk ? C_DONE
                    : prestige.estaDesbloqueada(h.padre) ? new Color(70, 80, 150)
                    : new Color(30, 35, 65);
                g2.setColor(lc);
                g2.setStroke(new BasicStroke(ambosOk ? 2.5f : 1.5f,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // Conectar borde derecho del nodo izquierdo con borde izquierdo del nodo derecho
                int px, py, cx, cy;
                if (cp[0] < pp[0]) {
                    // hijo a la izquierda
                    px = pp[0];       py = pp[1] + NH/2;
                    cx = cp[0] + NW;  cy = cp[1] + NH/2;
                } else {
                    // hijo a la derecha
                    px = pp[0] + NW;  py = pp[1] + NH/2;
                    cx = cp[0];       cy = cp[1] + NH/2;
                }
                // Línea con codo: horizontal hasta el centro, luego vertical, luego horizontal
                int midX = (px + cx) / 2;
                g2.drawLine(px, py, midX, py);
                g2.drawLine(midX, py, midX, cy);
                g2.drawLine(midX, cy, cx, cy);
            }
            g2.setStroke(new BasicStroke(1f));

            // Nodos (solo visibles)
            for (int i = 0; i < habs.length; i++) {
                if (!prestige.esVisible(habs[i])) continue;
                int[] p = nodePos(i);
                dibujarNodo(g2, habs[i], p[0], p[1], i == hoveredIdx);
            }
        }

        private void dibujarNodo(Graphics2D g2, PrestigioManager.Habilidad h, int x, int y, boolean hover) {
            boolean done   = prestige.estaDesbloqueada(h);
            boolean canBuy = prestige.sePuedeComprar(h);
            boolean esRaiz = h.esRaiz();

            Color bg     = done   ? new Color(15, 50, 30)
                         : canBuy ? C_CANBY
                         : esRaiz ? new Color(40, 30, 10) // raíz gratis sin comprar
                         :          C_LOCKED;
            Color border  = done   ? C_DONE
                          : canBuy ? (hover ? new Color(200, 140, 255) : C_PURPLE)
                          : esRaiz ? new Color(180, 130, 40)
                          :          new Color(45, 50, 80);

            // Sombra
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillRoundRect(x+3, y+3, NW, NH, 12, 12);
            // Fondo
            g2.setColor(bg);
            g2.fillRoundRect(x, y, NW, NH, 12, 12);
            if (hover && canBuy) {
                g2.setColor(new Color(200, 140, 255, 55));
                g2.fillRoundRect(x, y, NW, NH, 12, 12);
            }
            // Borde
            g2.setColor(border);
            g2.setStroke(new BasicStroke(done ? 2.2f : canBuy && hover ? 2.8f : 1.5f));
            g2.drawRoundRect(x, y, NW, NH, 12, 12);
            g2.setStroke(new BasicStroke(1f));

            // Nombre
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            Color nomColor = done ? C_DONE : canBuy ? new Color(180, 200, 255)
                           : esRaiz ? new Color(220, 180, 80) : new Color(70, 75, 110);
            g2.setColor(nomColor);
            FontMetrics fm = g2.getFontMetrics();
            String[] words = h.nombre.split(" ");
            String l1 = "", l2 = "";
            for (String w : words) {
                if (fm.stringWidth(l1 + " " + w) < NW - 14) l1 += (l1.isEmpty() ? "" : " ") + w;
                else l2 += (l2.isEmpty() ? "" : " ") + w;
            }
            int ty2 = y + (l2.isEmpty() ? NH/2 + fm.getAscent()/2 - 8 : NH/2 - 8);
            g2.drawString(l1, x + (NW - fm.stringWidth(l1))/2, ty2);
            if (!l2.isEmpty()) g2.drawString(l2, x + (NW - fm.stringWidth(l2))/2, ty2 + 14);

            // Costo / estado
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            fm = g2.getFontMetrics();
            String est = done ? "DESBLOQUEADO"
                       : h.costoBilletes == 0 ? "GRATIS"
                       : h.costoBilletes + " billete(s)";
            Color estCol = done ? C_DONE
                         : h.costoBilletes == 0 ? new Color(220, 180, 80)
                         : canBuy ? C_ACCENT : new Color(60, 65, 90);
            g2.setColor(estCol);
            g2.drawString(est, x + (NW - fm.stringWidth(est))/2, y + NH - 10);
        }
    }
}