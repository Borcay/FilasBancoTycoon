import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * Ventana del Pase de Batalla.
 * Muestra: línea de progreso horizontal con checkpoints, tabla de nivel actual/siguiente,
 * y animación completa de subida de nivel.
 */
public class VentanaPaseBatalla extends JDialog {

    // ── Paleta ──
    private static final Color BG_OSCURO      = new Color(15, 20, 35);
    private static final Color BG_PANEL       = new Color(22, 30, 50);
    private static final Color AZUL_VIVO      = new Color(30, 120, 255);
    private static final Color AZUL_CLARO     = new Color(80, 170, 255);
    private static final Color AZUL_GLOW      = new Color(30, 120, 255, 80);
    private static final Color DORADO         = new Color(255, 200, 50);
    private static final Color GRIS_COMPLETO  = new Color(60, 70, 90);
    private static final Color BLANCO         = Color.WHITE;
    private static final Color TEXTO_SUB      = new Color(160, 175, 200);
    private static final Color VERDE_REWARD   = new Color(50, 220, 120);
    private static final Color NARANJA_EVENTO = new Color(255, 150, 50);

    private final PaseBatalla pase;

    // ── Animación de subida de nivel ──
    volatile boolean animandoSubida = false;
    private volatile float   animAlpha      = 0f;
    private volatile float   animScale      = 0.5f;
    private volatile int     animNivel      = 0;
    private javax.swing.Timer animTimer;

    // ── Partículas de confeti ──
    private java.util.List<float[]> particulas = new java.util.ArrayList<>();

    public VentanaPaseBatalla(JFrame parent, PaseBatalla pase, boolean mostrarAnimacion) {
        super(parent, "Pase de Batalla", false);
        this.pase = pase;

        setSize(900, 580);
        setMinimumSize(new Dimension(700, 440));
        setLocationRelativeTo(parent);
        setResizable(true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_OSCURO);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            }
        };
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        root.add(crearHeader(), BorderLayout.NORTH);
        root.add(crearCuerpo(),  BorderLayout.CENTER);

        // Capa de animación encima de todo
        JLayeredPane layered = new JLayeredPane();
        layered.setPreferredSize(new Dimension(900, 580));
        root.setSize(900, 580);
        root.setBounds(0, 0, 900, 580);
        layered.add(root, JLayeredPane.DEFAULT_LAYER);

        final PanelAnimacion panelAnim = new PanelAnimacion() {
            // Solo intercepta eventos cuando la animación está activa
            @Override public boolean contains(int x, int y) {
                return animandoSubida && super.contains(x, y);
            }
        };
        panelAnim.setBounds(0, 0, 900, 580);
        panelAnim.setOpaque(false);
        panelAnim.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                cerrarAnimacion();
            }
        });
        layered.add(panelAnim, JLayeredPane.POPUP_LAYER);

        setContentPane(layered);

        // ══════════════════════════════════════════════
        //  DRAG & RESIZE — usando coordenadas de pantalla
        //  para máxima fluidez sin lag
        // ══════════════════════════════════════════════
        final int RZ = 20; // resize zone px
        // Estado compartido: [screenX, screenY, winX, winY, winW, winH, modo]
        // modo: 0=nada, 1=drag, 2=resize-SE, 3=resize-E, 4=resize-S
        final int[] estado = {0, 0, 0, 0, 0, 0, 0};

        java.awt.event.MouseAdapter dragResize = new java.awt.event.MouseAdapter() {

            private int getZona(MouseEvent e) {
                int x = e.getX(), y = e.getY(), w = getWidth(), h = getHeight();
                boolean enE = x >= w - RZ;
                boolean enS = y >= h - RZ;
                if (enE && enS) return 2; // esquina SE
                if (enE)        return 3; // borde derecho
                if (enS)        return 4; // borde inferior
                return 1;                 // arrastrar
            }

            @Override public void mousePressed(MouseEvent e) {
                int zona = getZona(e);
                estado[0] = e.getXOnScreen();
                estado[1] = e.getYOnScreen();
                estado[2] = getLocation().x;
                estado[3] = getLocation().y;
                estado[4] = getWidth();
                estado[5] = getHeight();
                estado[6] = zona;
            }

            @Override public void mouseReleased(MouseEvent e) {
                estado[6] = 0;
                setCursor(Cursor.getDefaultCursor());
            }

            @Override public void mouseMoved(MouseEvent e) {
                switch (getZona(e)) {
                    case 2 -> setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                    case 3 -> setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                    case 4 -> setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
                    default -> setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
            }

            @Override public void mouseDragged(MouseEvent e) {
                int dx = e.getXOnScreen() - estado[0];
                int dy = e.getYOnScreen() - estado[1];
                int zona = estado[6];
                if (zona == 1) {
                    // Mover ventana — directo con coordenadas de pantalla
                    setLocation(estado[2] + dx, estado[3] + dy);
                } else {
                    // Resize
                    int newW = estado[4], newH = estado[5];
                    if (zona == 2 || zona == 3) newW = Math.max(700, estado[4] + dx);
                    if (zona == 2 || zona == 4) newH = Math.max(440, estado[5] + dy);
                    setSize(newW, newH);
                    root.setBounds(0, 0, newW, newH);
                    panelAnim.setBounds(0, 0, newW, newH);
                    revalidate();
                }
            }
        };

        // Registrar en root (drag) y en layered (resize desde bordes)
        root.addMouseListener(dragResize);
        root.addMouseMotionListener(dragResize);
        layered.addMouseListener(dragResize);
        layered.addMouseMotionListener(dragResize);

        // Cerrar con ESC o clic fuera
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) dispose();
            }
        });
        setFocusable(true);

        // Iniciar timer de animación continua (para la barra y posibles efectos)
        animTimer = new javax.swing.Timer(16, e -> {
            if (animandoSubida) {
                animAlpha  += 0.04f;
                animScale  += (1.0f - animScale) * 0.12f;
                for (float[] p : particulas) {
                    p[0] += p[2]; p[1] += p[3]; p[3] += 0.3f; p[4] -= 0.015f;
                }
                particulas.removeIf(p -> p[4] <= 0 || p[1] > 580);
                if (animAlpha >= 1f && particulas.isEmpty() && animScale > 0.95f) {
                    // Mantener visible 2 segundos luego cerrar
                    animTimer.setDelay(2000);
                    animTimer.setRepeats(false);
                }
            }
            panelAnim.repaint();
            root.repaint();
        });
        animTimer.start();

        if (mostrarAnimacion) {
            iniciarAnimacionSubida(pase.getNivel());
        }
    }

    public void iniciarAnimacionSubida(int nuevoNivel) {
        animNivel     = nuevoNivel;
        animAlpha     = 0f;
        animScale     = 0.4f;
        animandoSubida = true;
        generarParticulas();
        animTimer.setDelay(16);
        animTimer.setRepeats(true);
        animTimer.restart();
    }

    private void generarParticulas() {
        particulas.clear();
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < 60; i++) {
            float[] p = new float[5]; // x, y, vx, vy, alpha
            p[0] = 200 + rnd.nextFloat() * 500;
            p[1] = 150 + rnd.nextFloat() * 100;
            p[2] = -3f + rnd.nextFloat() * 6f;
            p[3] = -4f - rnd.nextFloat() * 4f;
            p[4] = 0.8f + rnd.nextFloat() * 0.2f;
            particulas.add(p);
        }
    }

    // ── Header ──
    private JPanel crearHeader() {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(10, 40, 100),
                        getWidth(), 0, new Color(20, 80, 180));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight() + 20, 20, 20);
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(900, 65));
        p.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));

        JLabel titulo = new JLabel("⚔  PASE DE BATALLA");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titulo.setForeground(BLANCO);

        JLabel nivel = new JLabel("Nivel " + pase.getNivel() + " / " + PaseBatalla.MAX_NIVEL);
        nivel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        nivel.setForeground(AZUL_CLARO);

        JButton cerrar = new JButton("✕");
        cerrar.setFont(new Font("Segoe UI", Font.BOLD, 16));
        cerrar.setForeground(BLANCO);
        cerrar.setBackground(new Color(200, 60, 60));
        cerrar.setBorderPainted(false);
        cerrar.setFocusPainted(false);
        cerrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cerrar.setPreferredSize(new Dimension(36, 30));
        cerrar.addActionListener(e -> dispose());

        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        izq.setOpaque(false);
        izq.add(titulo);
        izq.add(nivel);

        p.add(izq,    BorderLayout.WEST);
        p.add(cerrar, BorderLayout.EAST);
        return p;
    }

    // ── Cuerpo principal ──
    private JPanel crearCuerpo() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                // Escala relativa al tamaño real del panel
                float sx = getWidth()  / 900f;
                float sy = getHeight() / 515f;
                float s  = Math.min(sx, sy); // escala uniforme
                g2.scale(s, s);
                dibujarProgreso(g2, (int)(getWidth() / s), (int)(getHeight() / s));
                dibujarTabla(g2, (int)(getWidth() / s), (int)(getHeight() / s));
            }
        };
        p.setOpaque(false);
        return p;
    }

    private void dibujarProgreso(Graphics2D g2, int PW, int PH) {
        int pad  = 20;
        int W    = PW - pad * 2;
        int x0   = pad;
        int y0   = 30;
        int nivelActual  = pase.getNivel();
        long xpActual    = pase.getXpEnNivelActual();
        long xpSiguiente = pase.getXpParaSiguiente();

        // Título
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.setColor(AZUL_CLARO);
        g2.drawString("PROGRESO DEL PASE", x0, y0);

        // XP info
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        g2.setColor(TEXTO_SUB);
        String xpStr = pase.esNivelMax()
            ? "¡Nivel Máximo alcanzado!"
            : xpActual + " / " + xpSiguiente + " XP  para el siguiente nivel";
        g2.drawString(xpStr, x0, y0 + 20);

        // Línea de checkpoints
        int lineaY   = y0 + 60;
        int lineaH   = 8;
        int checkR   = 14;
        int totalNiv = PaseBatalla.MAX_NIVEL;
        int pasoX    = W / (totalNiv - 1);

        // Barra base gris
        g2.setColor(GRIS_COMPLETO);
        g2.fillRoundRect(x0, lineaY - lineaH/2, W, lineaH, lineaH, lineaH);

        // Barra progreso azul
        double progreso = pase.esNivelMax() ? 1.0
            : (nivelActual - 1 + (xpSiguiente > 0 ? (double)xpActual / xpSiguiente : 0)) / (totalNiv - 1);
        int progresoW = (int)(W * progreso);

        g2.setColor(AZUL_GLOW);
        g2.fillRoundRect(x0, lineaY - lineaH/2 - 3, progresoW, lineaH + 6, lineaH, lineaH);
        GradientPaint gp = new GradientPaint(x0, 0, AZUL_VIVO, x0 + progresoW, 0, AZUL_CLARO);
        g2.setPaint(gp);
        g2.fillRoundRect(x0, lineaY - lineaH/2, progresoW, lineaH, lineaH, lineaH);

        // Checkpoints
        for (int i = 0; i < totalNiv; i++) {
            int cx = x0 + i * pasoX;
            boolean completado = i + 1 < nivelActual;
            boolean esActual   = i + 1 == nivelActual;
            PaseBatalla.Recompensa r = pase.getRecompensa(i + 1);
            Color colorCheck = completado ? AZUL_CLARO : (esActual ? DORADO : GRIS_COMPLETO);

            g2.setColor(completado ? AZUL_VIVO : (esActual ? DORADO : BG_PANEL));
            g2.fillOval(cx - checkR, lineaY - checkR, checkR*2, checkR*2);
            g2.setColor(colorCheck);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(cx - checkR, lineaY - checkR, checkR*2, checkR*2);
            g2.setStroke(new BasicStroke(1f));

            g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
            g2.setColor(completado || esActual ? BLANCO : TEXTO_SUB);
            String nStr = String.valueOf(i + 1);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(nStr, cx - fm.stringWidth(nStr)/2, lineaY + fm.getAscent()/2 - 1);

            if (r != null) {
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                String icono = iconoRecompensa(r.tipo);
                g2.setColor(completado ? VERDE_REWARD : (esActual ? DORADO : GRIS_COMPLETO));
                fm = g2.getFontMetrics();
                g2.drawString(icono, cx - fm.stringWidth(icono)/2, lineaY + checkR + 14);
            }
        }

        if (!pase.esNivelMax()) {
            int cx = x0 + (nivelActual - 1) * pasoX;
            g2.setColor(new Color(255, 220, 50, 120));
            g2.fillOval(cx - checkR - 5, lineaY - checkR - 5, (checkR+5)*2, (checkR+5)*2);
        }
    }

    private void dibujarTabla(Graphics2D g2, int PW, int PH) {
        int padding    = 20;
        int mitad      = PW / 2;
        int y0         = 175;
        int cuadroH    = PH - y0 - padding;

        int nivelActual = pase.getNivel();
        int nivelSig    = Math.min(nivelActual + 1, PaseBatalla.MAX_NIVEL);

        PaseBatalla.Recompensa rActual = pase.getRecompensa(nivelActual);
        PaseBatalla.Recompensa rSig    = pase.getRecompensa(nivelSig);

        dibujarCuadroNivel(g2, padding, y0, mitad - padding * 2, cuadroH,
            "NIVEL ACTUAL", nivelActual, rActual, true);
        dibujarCuadroNivel(g2, mitad + padding, y0, mitad - padding * 2, cuadroH,
            pase.esNivelMax() ? "NIVEL MÁXIMO" : "SIGUIENTE NIVEL",
            nivelSig, rSig, false);
    }

    private void dibujarCuadroNivel(Graphics2D g2, int x, int y, int w, int h,
            String etiqueta, int nivel, PaseBatalla.Recompensa r, boolean esActual) {

        // Fondo
        g2.setColor(esActual ? new Color(20, 40, 80) : new Color(25, 35, 60));
        g2.fillRoundRect(x, y, w, h, 16, 16);
        g2.setColor(esActual ? AZUL_VIVO : new Color(50, 65, 100));
        g2.setStroke(new BasicStroke(esActual ? 2.5f : 1.5f));
        g2.drawRoundRect(x, y, w, h, 16, 16);
        g2.setStroke(new BasicStroke(1f));

        // Etiqueta
        int fs11 = Math.max(9, (int)(11 * h / 270f));
        g2.setFont(new Font("Segoe UI", Font.BOLD, fs11));
        g2.setColor(esActual ? AZUL_CLARO : TEXTO_SUB);
        g2.drawString(etiqueta, x + 16, y + (int)(h * 0.08f));

        // Número nivel — proporcional a la altura del cuadro
        int fsNivel = Math.max(20, (int)(52 * h / 270f));
        g2.setFont(new Font("Segoe UI", Font.BOLD, fsNivel));
        g2.setColor(esActual ? DORADO : BLANCO);
        String nStr = String.valueOf(nivel);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(nStr, x + (w - fm.stringWidth(nStr)) / 2, y + (int)(h * 0.38f));

        // Ícono recompensa
        if (r != null) {
            int fsIcon = Math.max(14, (int)(32 * h / 270f));
            String icono = iconoRecompensaGrande(r.tipo);
            g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, fsIcon));
            fm = g2.getFontMetrics();
            g2.setColor(colorRecompensa(r.tipo));
            g2.drawString(icono, x + (w - fm.stringWidth(icono)) / 2, y + (int)(h * 0.58f));

            // Descripción
            int fsDesc = Math.max(9, (int)(14 * h / 270f));
            g2.setFont(new Font("Segoe UI", Font.BOLD, fsDesc));
            g2.setColor(colorRecompensa(r.tipo));
            fm = g2.getFontMetrics();
            String desc = r.descripcion;
            while (fm.stringWidth(desc) > w - 30 && desc.length() > 5)
                desc = desc.substring(0, desc.length() - 1);
            g2.drawString(desc, x + (w - fm.stringWidth(desc)) / 2, y + (int)(h * 0.72f));
        }

        // XP barra (solo nivel actual)
        if (esActual && !pase.esNivelMax()) {
            long xpAct = pase.getXpEnNivelActual();
            long xpNec = pase.getXpParaSiguiente();
            double prog = xpNec > 0 ? (double) xpAct / xpNec : 1.0;
            int bx = x + 16, bw = w - 32, bh = Math.max(6, (int)(10 * h / 270f));
            int by = y + (int)(h * 0.82f);
            g2.setColor(GRIS_COMPLETO);
            g2.fillRoundRect(bx, by, bw, bh, bh, bh);
            g2.setColor(AZUL_VIVO);
            g2.fillRoundRect(bx, by, (int)(bw * prog), bh, bh, bh);
            int fsXp = Math.max(8, (int)(12 * h / 270f));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, fsXp));
            g2.setColor(TEXTO_SUB);
            String xpStr = xpAct + " / " + xpNec + " XP";
            fm = g2.getFontMetrics();
            g2.drawString(xpStr, x + (w - fm.stringWidth(xpStr)) / 2, by + bh + 14);
        } else if (esActual && pase.esNivelMax()) {
            int fsMax = Math.max(9, (int)(13 * h / 270f));
            g2.setFont(new Font("Segoe UI", Font.BOLD, fsMax));
            g2.setColor(DORADO);
            String msg = "¡Nivel máximo alcanzado!";
            fm = g2.getFontMetrics();
            g2.drawString(msg, x + (w - fm.stringWidth(msg)) / 2, y + (int)(h * 0.90f));
        }
    }

    private String iconoRecompensa(PaseBatalla.TipoRecompensa tipo) {
        return switch (tipo) {
            case MONEDAS          -> "$";
            case MEJORA_VELOCIDAD -> "⚡";
            case MEJORA_FLUJO     -> "↑";
            case CAJERO_EXTRA     -> "+C";
            case EVENTO_HORA_PICO -> "🔥";
            case EVENTO_DIA_VIP   -> "★";
        };
    }

    private String iconoRecompensaGrande(PaseBatalla.TipoRecompensa tipo) {
        return switch (tipo) {
            case MONEDAS          -> "💰";
            case MEJORA_VELOCIDAD -> "⚡";
            case MEJORA_FLUJO     -> "🌊";
            case CAJERO_EXTRA     -> "🏦";
            case EVENTO_HORA_PICO -> "🔥";
            case EVENTO_DIA_VIP   -> "⭐";
        };
    }

    private Color colorRecompensa(PaseBatalla.TipoRecompensa tipo) {
        return switch (tipo) {
            case MONEDAS          -> DORADO;
            case MEJORA_VELOCIDAD -> new Color(100, 200, 255);
            case MEJORA_FLUJO     -> new Color(80, 220, 180);
            case CAJERO_EXTRA     -> VERDE_REWARD;
            case EVENTO_HORA_PICO -> NARANJA_EVENTO;
            case EVENTO_DIA_VIP   -> new Color(220, 150, 255);
        };
    }

    // ── Panel de animación de subida de nivel (capa superior) ──
    class PanelAnimacion extends JPanel {
        @Override protected void paintComponent(Graphics g) {
            if (!animandoSubida) return;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            float alpha = Math.min(1f, animAlpha);

            // Overlay oscuro semitransparente
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(0.75f, alpha)));
            g2.setColor(new Color(5, 10, 25));
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Partículas de confeti
            for (float[] p : particulas) {
                if (p[4] <= 0) continue;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p[4]));
                Color[] colores = {DORADO, AZUL_CLARO, VERDE_REWARD, NARANJA_EVENTO, Color.WHITE};
                g2.setColor(colores[(int)(p[0] * 7) % colores.length]);
                g2.fillOval((int)p[0], (int)p[1], 8, 8);
            }

            // Panel central
            int pw = 420, ph = 260;
            int px = (getWidth() - pw) / 2, py = (getHeight() - ph) / 2;

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.translate(getWidth()/2, getHeight()/2);
            g2.scale(animScale, animScale);
            g2.translate(-getWidth()/2, -getHeight()/2);

            // Fondo del panel con glow
            g2.setColor(new Color(20, 50, 120, 200));
            g2.fillRoundRect(px - 10, py - 10, pw + 20, ph + 20, 24, 24);
            g2.setColor(AZUL_VIVO);
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(px - 10, py - 10, pw + 20, ph + 20, 24, 24);
            g2.setStroke(new BasicStroke(1f));

            // Título
            g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
            g2.setColor(AZUL_CLARO);
            String sub = "¡SUBISTE DE NIVEL!";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(sub, getWidth()/2 - fm.stringWidth(sub)/2, py + 40);

            // Número de nivel grande
            g2.setFont(new Font("Segoe UI", Font.BOLD, 90));
            g2.setColor(DORADO);
            String nStr = String.valueOf(animNivel);
            fm = g2.getFontMetrics();
            g2.drawString(nStr, getWidth()/2 - fm.stringWidth(nStr)/2, py + 155);

            // Recompensa
            PaseBatalla.Recompensa r = pase.getRecompensa(animNivel);
            if (r != null) {
                g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
                g2.setColor(colorRecompensa(r.tipo));
                String rStr = "🎁  " + r.descripcion;
                fm = g2.getFontMetrics();
                g2.drawString(rStr, getWidth()/2 - fm.stringWidth(rStr)/2, py + 195);
            }

            // Botón cerrar
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.setColor(TEXTO_SUB);
            String cerrarStr = "[ clic para cerrar animación ]";
            fm = g2.getFontMetrics();
            g2.drawString(cerrarStr, getWidth()/2 - fm.stringWidth(cerrarStr)/2, py + 240);

            g2.setTransform(new java.awt.geom.AffineTransform());
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        // Triángulo de resize siempre visible en esquina inferior derecha
        @Override public void paintChildren(Graphics g) {
            super.paintChildren(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), s = 16;
            g2.setColor(new Color(255,255,255,60));
            g2.fillPolygon(new int[]{w-s, w, w}, new int[]{h, h-s, h}, 3);
            g2.setColor(new Color(255,255,255,120));
            g2.drawLine(w-s+2, h-2, w-2, h-s+2);
            g2.drawLine(w-s/2+2, h-2, w-2, h-s/2+2);
        }
    }

    /** Cierra la animación si el jugador hace clic */
    public void cerrarAnimacion() {
        animandoSubida = false;
        animTimer.stop();
        repaint();
    }
}
