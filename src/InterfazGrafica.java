import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InterfazGrafica extends JFrame {

    // ── Paleta ──
    private static final Color C_FONDO        = new Color(245, 248, 255);
    private static final Color C_CARRIL_A     = new Color(230, 238, 255);
    private static final Color C_CARRIL_B     = new Color(238, 244, 255);
    private static final Color C_SEP          = new Color(200, 215, 240);
    private static final Color C_CAJERO_OPEN  = new Color(180, 220, 255);
    private static final Color C_CAJERO_FAST  = new Color(140, 255, 180);
    private static final Color C_CAJERO_CLOSE = new Color(255, 100, 100);
    private static final Color C_CLIENTE      = new Color(255, 220, 160);
    private static final Color C_CLIENTE_SRV  = new Color(180, 255, 200);
    private static final Color C_CLIENTE_VIP  = new Color(255, 240, 150);
    private static final Color C_CLIENTE_VSRV = new Color(160, 255, 190);
    private static final Color C_HEADER1      = new Color(100, 150, 220);
    private static final Color C_HEADER2      = new Color(80,  120, 200);
    private static final Color C_TXT_CAJERO   = new Color(30,  60,  120);
    private static final Color C_TXT_CLIENTE  = new Color(100, 60,  10);
    private static final Color C_PROG_GREEN   = new Color(140, 255, 180);
    private static final Color C_BOT_BG       = new Color(234, 240, 255);
    private static final Color C_BOT_BORDER   = new Color(184, 204, 238);
    private static final Color C_GOLD         = new Color(220, 160,  10);
    private static final Color C_GREEN_UPG    = new Color( 26, 154,  80);
    private static final Color C_UPG_BG       = new Color(244, 248, 255);
    private static final Color C_UPG_HOVER    = new Color(210, 230, 255);
    private static final Color C_UPG_LOCKED   = new Color(220, 225, 240);
    private static final Color C_UPG_MAX_BG   = new Color(221, 255, 240);
    private static final Color C_MUS_BG       = new Color( 30,  40,  70);
    private static final Color C_MUS_ACCENT   = new Color(140, 170, 238);

    // ── Medidas ──
    private static final int CARRIL_ALTO = 82;
    private static final int HEADER_ALTO = 70;
    private static final int DAYBAR_ALTO = 32;
    private static final int BOT_ALTO    = 240;
    private static final int CAJERO_W   = 90;
    private static final int CAJERO_H   = 56;
    private static final int CLI_W      = 72;
    private static final int CLI_H      = 46;
    private static final int CLI_GAP    = 8;
    private static final int PAD        = 14;
    private static final double LERP    = 0.14;

    private SimulacionBanco sim;
    private Economia eco;
    private final SonidoManager sonido;

    // ── Animación de clientes ──
    // posiciones interpoladas: clienteId -> [x, y]
    private final ConcurrentHashMap<Integer, double[]> posiciones = new ConcurrentHashMap<>();

    // clientes en fade-out: clienteId -> [alpha, yOffset, nombre, x, y, vip(0/1), srv(0/1)]
    private final ConcurrentHashMap<Integer, double[]> fadeOuts = new ConcurrentHashMap<>();

    // ── Popups de monedas: [x, y, alpha, valor, vip] ──
    private final CopyOnWriteArrayList<double[]> coinPopups = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Object[]> toasts     = new CopyOnWriteArrayList<>();

    // ── Overlay de cambio de día ──
    private volatile boolean mostrandoOverlayDia = false;
    private volatile String  textOverlay1 = "";
    private volatile String  textOverlay2 = "";
    private volatile String  textOverlay3 = "";  // reporte ladrones
    private volatile float   alphaOverlay = 0f;
    private volatile boolean overlaySubiendo = true;

    private PanelBanco   panelBanco;
    private PanelDayBar  panelDayBar;
    private javax.swing.Timer animTimer;

    // ── Header labels ──
    private JLabel lblContadorClientes; // "Clientes: X / 200"
    private JLabel lblTimerHeader;      // "Tiempo: 00:00"
    private JLabel lblMonedasHeader;    // "$1234" (grande, dorado)

    // ── Stats panel ──
    private JLabel lblGanHoy, lblMejorDia, lblNumCajeros, lblVelocidad, lblTiempoJuego;

    // ── Mejoras ──
    private JPanel[] panelesMejora;
    private static final String[] UPG_NOMBRES = {"Nuevo Cajero","Mayor Afluencia","Cajero Veloz","Comision Mayor","Sala VIP"};
    private static final String[] UPG_DESCS   = {
        "Agrega una ventanilla",
        "Mas clientes por minuto",
        "Reduce tiempo de atencion",
        "Mas monedas por cliente",
        "Clientes VIP dan x2 monedas"
    };
    // Descripción detallada que se muestra al hacer hover
    private static final String[] UPG_EFECTOS = {
        "+1 cajero activo (max 5 en total)",
        "Intervalo entre clientes: 2.1s → 0.4s (-420ms por nivel)",
        "Tiempo de atención: 4.0s → 0.9s (-720ms por nivel)",
        "Monedas por cliente: $10 → $42 (+$8 por nivel)",
        "Probabilidad VIP: +10% por nivel (max 30%)"
    };

    // ── Reproductor ──
    private JLabel  lblCancion;
    private JButton btnPausa;

    // ── Pase de Batalla ──
    private JPanel  pnlXpIndicador;
    private JLabel  lblXpNivel;
    private JLabel  lblXpProgress;

    // ── Prestige ──
    private JLabel  lblBilletes;
    private JButton btnBancos;
    private VentanaBancos ventanaBancos;

    // ── Panel lateral de bancos ──
    private JPanel panelLateralBancos;  // visible tras primer prestige
    private JScrollPane scrollBanco; // Bug19: referencia para redespachar rueda del mouse
    // ── Notificación de subida de nivel (ahora en glass pane) ──
    private volatile boolean mostrandoNotifNivel = false;
    private volatile int     notifNivel          = 0;
    private volatile float   notifAlpha          = 0f;
    private volatile boolean notifSubiendo       = true;
    private volatile long    notifInicioMs       = 0;
    // Bug12: botón de prestige manual visible tras alcanzar meta
    private volatile boolean mostrarBtnPrestige  = false;

    public InterfazGrafica(SimulacionBanco sim, Economia eco) {
        this.sim    = sim;
        this.eco    = eco;
        this.sonido = sim.getSonido();
        configurarVentana();
        construirUI();
        iniciarAnimacion();
        sonido.setOnCancionCambia(() -> SwingUtilities.invokeLater(this::actualizarReproductor));
        registrarAtajosTeclado();
    }

    private void registrarAtajosTeclado() {
        // 1-9 = bancos 1-9, 0 = banco 10
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addKeyEventDispatcher(e -> {
                if (e.getID() != java.awt.event.KeyEvent.KEY_PRESSED) return false;
                char c = e.getKeyChar();
                if (c >= '1' && c <= '9') {
                    int idx = c - '1'; // '1'->0, '2'->1, ...
                    if (onCambiarBanco != null) onCambiarBanco.accept(idx);
                    return true;
                }
                if (c == '0') {
                    if (onCambiarBanco != null) onCambiarBanco.accept(9); // 0 = banco 10
                    return true;
                }
                return false;
            });
    }

    private void configurarVentana() {
        setTitle("Banco Tycoon");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setBackground(C_FONDO);
        setSize(1100, HEADER_ALTO + DAYBAR_ALTO + CARRIL_ALTO * 2 + BOT_ALTO + 38);
        setLocationRelativeTo(null);
    }

    // ══════════════════════════════════════════════════
    //  CONSTRUCCION UI
    // ══════════════════════════════════════════════════
    private void construirUI() {
        setLayout(new BorderLayout());
        add(crearHeader(),        BorderLayout.NORTH);

        // Wrapper central: panel lateral (oculto inicialmente) + contenido del banco
        JPanel wrapper = new JPanel(new BorderLayout());
        panelLateralBancos = crearPanelLateral();
        panelLateralBancos.setVisible(false);
        wrapper.add(panelLateralBancos, BorderLayout.WEST);
        wrapper.add(crearCentro(),      BorderLayout.CENTER);

        add(wrapper,              BorderLayout.CENTER);
        add(crearPanelInferior(), BorderLayout.SOUTH);
    }

    // ══════════════════════════════════════════════════
    //  PANEL LATERAL DE BANCOS
    // ══════════════════════════════════════════════════
    private JPanel crearPanelLateral() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(14, 20, 50));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setPreferredSize(new Dimension(72, 0));
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(50, 70, 130)));
        return p;
    }

    /** Reconstruye los cubos del panel lateral con la lista actual de bancos */
    public void refrescarPanelLateral(java.util.List<SimulacionBanco> sims) {
        if (panelLateralBancos == null) return;
        panelLateralBancos.removeAll();
        panelLateralBancos.add(Box.createVerticalStrut(10));

        for (int i = 0; i < sims.size(); i++) {
            final SimulacionBanco s = sims.get(i);
            final int idx = i;
            JPanel cubo = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean activo = (s == sim);
                    Color bg = activo ? new Color(60, 100, 200)
                             : s.isTerminado() ? new Color(30, 100, 60)
                             : new Color(30, 40, 80);
                    Color border = activo ? new Color(140, 180, 255)
                                 : s.isTerminado() ? new Color(80, 200, 120)
                                 : new Color(60, 80, 140);
                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(border);
                    g2.setStroke(new BasicStroke(activo ? 2.5f : 1.5f));
                    g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 10, 10);
                    g2.setStroke(new BasicStroke(1f));
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    g2.setColor(Color.WHITE);
                    String n = String.valueOf(idx + 1);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(n, (getWidth()-fm.stringWidth(n))/2, getHeight()/2 - 2);
                    int bx = 4, bw = getWidth()-8, bh = 4, by = getHeight()-8;
                    g2.setColor(new Color(255,255,255,40));
                    g2.fillRoundRect(bx, by, bw, bh, 3, 3);
                    double pct = Math.min(1.0, (double)s.getClientesAtendidosTotal()/s.getMetaClientes());
                    g2.setColor(s.isTerminado() ? new Color(80,200,120) : new Color(100,160,255));
                    g2.fillRoundRect(bx, by, (int)(bw*pct), bh, 3, 3);
                    // Mejora6: indicador "P" morado si puede prestigiar
                    if (s.isMetaAlcanzada() && !s.isTerminado()) {
                        g2.setColor(new Color(180, 80, 255));
                        g2.fillOval(getWidth()-14, 2, 12, 12);
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                        g2.drawString("P", getWidth()-10, 11);
                    }
                }
                @Override public Dimension getPreferredSize() { return new Dimension(52, 52); }
                @Override public Dimension getMaximumSize()   { return new Dimension(52, 52); }
            };
            cubo.setOpaque(false);
            cubo.setAlignmentX(Component.CENTER_ALIGNMENT);
            cubo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            // Sin tooltip
            cubo.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (onCambiarBanco != null) onCambiarBanco.accept(idx);
                }
            });
            panelLateralBancos.add(cubo);
            panelLateralBancos.add(Box.createVerticalStrut(8));
        }

        // Separador
        panelLateralBancos.add(Box.createVerticalGlue());

        // Botón comprar banco
        PrestigioManager pr = sim.getPrestigio();
        if (pr != null) {
            JPanel btnComprar = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean puedeComprar = pr.getBilletes() >= pr.costoBanco();
                    g2.setColor(puedeComprar ? new Color(60, 30, 120) : new Color(30, 30, 50));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(puedeComprar ? new Color(160, 100, 255) : new Color(60, 60, 90));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 8, 8);
                    g2.setStroke(new BasicStroke(1f));
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    g2.setColor(puedeComprar ? Color.WHITE : new Color(100, 100, 130));
                    String l1 = "+";
                    String l2 = pr.costoBanco() + "B";
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(l1, (getWidth()-fm.stringWidth(l1))/2, getHeight()/2 - 3);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                    fm = g2.getFontMetrics();
                    g2.drawString(l2, (getWidth()-fm.stringWidth(l2))/2, getHeight()/2 + 10);
                }
                @Override public Dimension getPreferredSize() { return new Dimension(52, 44); }
                @Override public Dimension getMaximumSize()   { return new Dimension(52, 44); }
            };
            btnComprar.setOpaque(false);
            btnComprar.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnComprar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnComprar.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (onCompraBancoLateral != null) onCompraBancoLateral.run();
                }
            });
            panelLateralBancos.add(btnComprar);
            panelLateralBancos.add(Box.createVerticalStrut(8));
        }

        panelLateralBancos.revalidate();
        panelLateralBancos.repaint();
    }

    private Runnable onCompraBancoLateral;
    public void setOnCompraBancoLateral(Runnable r) { onCompraBancoLateral = r; }

    // Interfaces simples para evitar dependencia de java.util.function
    public interface BancoIndexConsumer { void accept(int idx); }
    public interface BancoIndexFunction  { int apply(SimulacionBanco s); }

    private BancoIndexConsumer onCambiarBanco;
    public void setOnCambiarBanco(BancoIndexConsumer c) { onCambiarBanco = c; }

    private BancoIndexFunction onObtenerIndiceBanco;
    public void setOnObtenerIndiceBanco(BancoIndexFunction f) { onObtenerIndiceBanco = f; }

    /** Cambia la vista al banco indicado (lo llama Main) */
    public void cambiarVistaBanco(SimulacionBanco nuevoSim, Economia nuevoEco) {
        SimulacionBanco anterior = this.sim;
        if (anterior != null && anterior != nuevoSim) anterior.setGui(null);

        this.sim = nuevoSim;
        this.eco = nuevoEco;

        nuevoSim.setGui(this);

        posiciones.clear();
        fadeOuts.clear();
        coinPopups.clear();

        SwingUtilities.invokeLater(() -> {
            actualizarPanelInferior();
            mostrarToast("Viendo Banco " + (obtenerIndiceBanco(nuevoSim) + 1), new Color(100, 160, 255));
            // Bug3: si el banco ya alcanzó la meta pero el diálogo no salió, mostrarlo ahora
            if (nuevoSim.isMetaAlcanzada() && !nuevoSim.isTerminado()) {
                nuevoSim.ofrecerPrestigioAhora();
            }
        });
    }

    private int obtenerIndiceBanco(SimulacionBanco s) {
        if (onObtenerIndiceBanco != null) return onObtenerIndiceBanco.apply(s);
        return 0;
    }

    // ── HEADER ──
    private JPanel crearHeader() {
        // Dibujamos todo a mano para evitar layouts complejos con null layout
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0,0,C_HEADER1, getWidth(),0,C_HEADER2));
                g2.fillRect(0,0,getWidth(),getHeight());

                // Barra de progreso global
                int bx = 24, by = 50, bw = 280, bh = 10;
                g2.setColor(new Color(255,255,255,50));
                g2.fillRoundRect(bx, by, bw, bh, 6, 6);
                int prog = (int)(bw * Math.min(1.0,
                    (double)sim.getClientesAtendidosTotal() / sim.getMetaClientes()));
                g2.setColor(C_PROG_GREEN);
                if (prog > 0) g2.fillRoundRect(bx, by, prog, bh, 6, 6);
            }
        };
        p.setPreferredSize(new Dimension(0, HEADER_ALTO));
        p.setOpaque(false);

        // Izquierda
        JPanel izq = new JPanel();
        izq.setLayout(new BoxLayout(izq, BoxLayout.Y_AXIS));
        izq.setOpaque(false);
        izq.setBorder(new EmptyBorder(10, 24, 6, 12));

        JLabel titulo = new JLabel("Banco Tycoon");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(Color.WHITE);
        izq.add(titulo);

        lblContadorClientes = new JLabel("Clientes Atendidos: 0 / 200");
        lblContadorClientes.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblContadorClientes.setForeground(new Color(255,255,255,210));
        izq.add(lblContadorClientes);
        izq.add(Box.createVerticalStrut(12)); // espacio para la barra

        p.add(izq, BorderLayout.WEST);

        // Centro: monedas (panel con fondo semitransparente)
        JPanel centroMon = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255,255,255,35));
                g2.fillRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
                g2.setColor(new Color(255,255,255,70));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
            }
        };
        centroMon.setLayout(new BoxLayout(centroMon, BoxLayout.Y_AXIS));
        centroMon.setOpaque(false);
        centroMon.setBorder(new EmptyBorder(8,20,8,20));

        JLabel monLbl = new JLabel("MONEDAS");
        monLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        monLbl.setForeground(new Color(255,255,255,170));
        monLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblMonedasHeader = new JLabel("$0");
        lblMonedasHeader.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblMonedasHeader.setForeground(new Color(255, 230, 60));
        lblMonedasHeader.setAlignmentX(Component.CENTER_ALIGNMENT);

        centroMon.add(monLbl);
        centroMon.add(lblMonedasHeader);

        // ── Mini indicador XP (amarillo-naranja, al lado de monedas) ──
        pnlXpIndicador = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                // Sombra exterior
                g2.setColor(new Color(0,0,0,60));
                g2.fillRoundRect(3, 3, w-2, h-2, 14, 14);

                // Fondo degradado amarillo-naranja
                GradientPaint gpx = new GradientPaint(0, 0, new Color(255, 160, 20), w, h, new Color(220, 80, 0));
                g2.setPaint(gpx);
                g2.fillRoundRect(0, 0, w-1, h-1, 14, 14);

                // Brillo superior
                g2.setColor(new Color(255,255,255,55));
                g2.fillRoundRect(4, 4, w-8, h/2-4, 10, 10);

                // Borde dorado
                g2.setColor(new Color(255, 220, 80, 200));
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawRoundRect(1, 1, w-3, h-3, 13, 13);
                g2.setStroke(new BasicStroke(1f));

                // Ícono espada
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
                g2.setColor(new Color(255, 240, 120));
                g2.drawString("⚔", 8, h/2 + 6);

                // Barra XP
                PaseBatalla pb = sim.getPaseBatalla();
                if (pb != null && !pb.esNivelMax()) {
                    long xpA = pb.getXpEnNivelActual(), xpN = pb.getXpParaSiguiente();
                    double prog = xpN > 0 ? (double)xpA/xpN : 1.0;
                    int bx=8, by=h-10, bw=w-16, bh=6;
                    g2.setColor(new Color(0,0,0,60));
                    g2.fillRoundRect(bx, by, bw, bh, 4, 4);
                    g2.setColor(new Color(255,255,255,200));
                    g2.fillRoundRect(bx, by, (int)(bw*prog), bh, 4, 4);
                    g2.setColor(new Color(0,0,0,40));
                    g2.drawRoundRect(bx, by, bw, bh, 4, 4);
                }
            }
        };
        pnlXpIndicador.setLayout(null);
        pnlXpIndicador.setOpaque(false);
        pnlXpIndicador.setPreferredSize(new Dimension(160, 58));
        pnlXpIndicador.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        lblXpNivel = new JLabel("NIVEL 1");
        lblXpNivel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblXpNivel.setForeground(new Color(255, 250, 200));
        lblXpNivel.setBounds(32, 8, 120, 20);

        lblXpProgress = new JLabel("0 / 100 XP");
        lblXpProgress.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblXpProgress.setForeground(new Color(255, 240, 180));
        lblXpProgress.setBounds(32, 26, 120, 16);

        pnlXpIndicador.add(lblXpNivel);
        pnlXpIndicador.add(lblXpProgress);
        pnlXpIndicador.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { abrirVentanaPaseBatalla(false); }
        });

        // Centrar ambos cuadros (monedas + XP) juntos
        JPanel centroWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 6));
        centroWrap.setOpaque(false);

        // Panel billetes (solo visible tras primer prestige)
        JPanel panelBilletes = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(80, 30, 140, 180));
                g2.fillRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
                g2.setColor(new Color(180,120,255,160));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
            }
        };
        panelBilletes.setLayout(new BoxLayout(panelBilletes, BoxLayout.Y_AXIS));
        panelBilletes.setOpaque(false);
        panelBilletes.setBorder(new EmptyBorder(6,14,6,14));
        panelBilletes.setVisible(false); // oculto hasta primer prestige

        JLabel billeteLbl = new JLabel("BILLETES");
        billeteLbl.setFont(new Font("Segoe UI", Font.BOLD, 9));
        billeteLbl.setForeground(new Color(200,160,255,200));
        billeteLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblBilletes = new JLabel("0 B");
        lblBilletes.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblBilletes.setForeground(new Color(210, 150, 255));
        lblBilletes.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblBilletes.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblBilletes.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { abrirArbolHabilidades(); }
        });

        panelBilletes.add(billeteLbl);
        panelBilletes.add(lblBilletes);

        centroWrap.add(centroMon);
        centroWrap.add(pnlXpIndicador);
        centroWrap.add(panelBilletes);
        // Guardar referencia para hacerlo visible tras prestige
        centroWrap.putClientProperty("panelBilletes", panelBilletes);
        p.putClientProperty("centroWrap", centroWrap);
        p.add(centroWrap, BorderLayout.CENTER);

        // Derecha: timer + botón bancos

        JPanel der = new JPanel();
        der.setLayout(new BoxLayout(der, BoxLayout.Y_AXIS));
        der.setOpaque(false);
        der.setBorder(new EmptyBorder(10, 8, 8, 20));

        lblTimerHeader = new JLabel("Tiempo: 00:00");
        lblTimerHeader.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblTimerHeader.setForeground(new Color(255,255,255,210));
        lblTimerHeader.setAlignmentX(Component.RIGHT_ALIGNMENT);
        der.add(lblTimerHeader);

        der.add(Box.createVerticalStrut(4));

        btnBancos = new JButton("Mis Bancos");
        btnBancos.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnBancos.setForeground(Color.WHITE);
        btnBancos.setBackground(new Color(80, 50, 160));
        btnBancos.setBorderPainted(false);
        btnBancos.setFocusPainted(false);
        btnBancos.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBancos.setAlignmentX(Component.RIGHT_ALIGNMENT);
        btnBancos.setVisible(false); // oculto hasta primer prestige
        btnBancos.addActionListener(e -> abrirVentanaBancos());
        der.add(btnBancos);

        der.add(Box.createVerticalStrut(4));

        p.add(der, BorderLayout.EAST);
        return p;
    }

    private JPanel dotLeyenda(Color c, String txt) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        p.setOpaque(false);
        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c);
                g2.fillRoundRect(0,2,12,12,4,4);
                g2.setColor(new Color(0,0,0,35));
                g2.drawRoundRect(0,2,11,11,4,4);
            }
        };
        dot.setPreferredSize(new Dimension(12, 16));
        dot.setOpaque(false);
        JLabel l = new JLabel(txt);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(new Color(255,255,255,200));
        p.add(dot); p.add(l);
        return p;
    }

    // ── CENTRO ──
    private JPanel crearCentro() {
        JPanel c = new JPanel(new BorderLayout());
        c.setBackground(C_FONDO);

        panelDayBar = new PanelDayBar();
        panelDayBar.setPreferredSize(new Dimension(0, DAYBAR_ALTO));
        c.add(panelDayBar, BorderLayout.NORTH);

        panelBanco = new PanelBanco();
        scrollBanco = new JScrollPane(panelBanco,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollBanco.setBorder(null);
        scrollBanco.getViewport().setBackground(C_FONDO);
        c.add(scrollBanco, BorderLayout.CENTER);

        // Bug11: instalar glass pane para dibujar notificación siempre visible
        JPanel glass = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                // Solo dibujamos la notif de nivel (y el botón de prestige manual)
                if ((!mostrandoNotifNivel || notifAlpha <= 0) && !mostrarBtnPrestige) return;
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                if (mostrandoNotifNivel && notifAlpha > 0) dibujarNotifNivel(g2, getWidth(), getHeight());
                if (mostrarBtnPrestige) dibujarBotonPrestige(g2, getWidth(), getHeight());
            }
        };
        glass.setOpaque(false);

        // CRÍTICO: el glass pane solo consume clicks que caen sobre sus propias zonas activas.
        // Todo lo demás se redespecha al componente que hay debajo.
        MouseAdapter glassListener = new MouseAdapter() {
            private boolean enZonaActiva(MouseEvent e) {
                int x = e.getX(), y = e.getY();
                if (mostrarBtnPrestige) {
                    int bw = 190, bh = 36;
                    int bx = glass.getWidth() - bw - 16;
                    int by = glass.getHeight() - bh - 16;
                    if (x >= bx && x <= bx+bw && y >= by && y <= by+bh) return true;
                }
                if (mostrandoNotifNivel && notifAlpha > 0.1f) {
                    int nw = 340, nh = 90;
                    int nx = glass.getWidth() - nw - 16, ny = 16;
                    if (x >= nx && x <= nx+nw && y >= ny && y <= ny+nh) return true;
                }
                return false;
            }

            private void redespacher(MouseEvent e) {
                if (enZonaActiva(e)) return; // consumir
                // Pasar el evento al componente real debajo del glass pane
                Component dest = getContentPane().findComponentAt(e.getX(), e.getY());
                if (dest != null && dest != glass) {
                    dest.dispatchEvent(SwingUtilities.convertMouseEvent(glass, e, dest));
                }
            }

            @Override public void mouseClicked(MouseEvent e) {
                if (!enZonaActiva(e)) { redespacher(e); return; }
                int x = e.getX(), y = e.getY();
                if (mostrarBtnPrestige) {
                    int bw = 190, bh = 36;
                    int bx = glass.getWidth() - bw - 16;
                    int by = glass.getHeight() - bh - 16;
                    if (x >= bx && x <= bx+bw && y >= by && y <= by+bh) {
                        sim.prestigiarAhora(); return;
                    }
                }
                if (mostrandoNotifNivel && notifAlpha > 0.1f) {
                    int nw = 340, nh = 90;
                    int nx = glass.getWidth() - nw - 16, ny = 16;
                    if (x >= nx && x <= nx+nw && y >= ny && y <= ny+nh) {
                        abrirVentanaPaseBatalla(true);
                    }
                }
            }
            @Override public void mousePressed(MouseEvent e)  { redespacher(e); }
            @Override public void mouseReleased(MouseEvent e) { redespacher(e); }
            @Override public void mouseMoved(MouseEvent e)    { redespacher(e); }
            @Override public void mouseDragged(MouseEvent e)  { redespacher(e); }
        };
        glass.addMouseListener(glassListener);
        glass.addMouseMotionListener(glassListener);
        // Bug19: redespachar rueda directamente al scrollBanco
        glass.addMouseWheelListener(e -> {
            if (scrollBanco != null)
                scrollBanco.dispatchEvent(SwingUtilities.convertMouseEvent(glass, e, scrollBanco));
        });
        setGlassPane(glass);
        glass.setVisible(true);
        return c;
    }

    // ══════════════════════════════════════════════════
    //  PANEL INFERIOR
    // ══════════════════════════════════════════════════
    private JPanel crearPanelInferior() {
        JPanel bot = new JPanel(new BorderLayout());
        bot.setPreferredSize(new Dimension(0, BOT_ALTO));
        bot.setBackground(C_BOT_BG);
        bot.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, C_BOT_BORDER));

        JPanel stats = crearPanelStats();
        stats.setPreferredSize(new Dimension(175, BOT_ALTO));
        bot.add(stats, BorderLayout.WEST);

        bot.add(crearPanelMejoras(), BorderLayout.CENTER);

        JPanel mus = crearReproductor();
        mus.setPreferredSize(new Dimension(215, BOT_ALTO));
        bot.add(mus, BorderLayout.EAST);
        return bot;
    }

    // ── Stats ──
    private JPanel crearPanelStats() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(C_BOT_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,0,1,C_BOT_BORDER),
            new EmptyBorder(8,10,8,10)));

        JLabel tit = new JLabel("ESTADÍSTICAS");
        tit.setFont(new Font("Segoe UI", Font.BOLD, 9));
        tit.setForeground(new Color(112,144,192));
        tit.setAlignmentX(Component.LEFT_ALIGNMENT);
        tit.setBorder(new EmptyBorder(0,0,5,0));
        p.add(tit);

        lblGanHoy      = mkStatBox(p, "Ganancias hoy",  "$0",    C_GOLD);
        lblMejorDia    = mkStatBox(p, "Mejor dia",      "$0",    new Color(48,96,192));
        lblNumCajeros  = mkStatBox(p, "Cajeros activos","1",     C_GREEN_UPG);
        lblVelocidad   = mkStatBox(p, "Tiempo atencion", "4.0s",  new Color(180,40,40));
        lblTiempoJuego = mkStatBox(p, "Tiempo total",   "00:00", new Color(80,80,160));
        return p;
    }

    private JLabel mkStatBox(JPanel parent, String label, String val, Color color) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(Color.WHITE);
        box.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BOT_BORDER, 1),
            new EmptyBorder(3,7,3,7)));
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel lbl = new JLabel(label.toUpperCase());
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 8));
        lbl.setForeground(new Color(112,144,192));

        JLabel v = new JLabel(val);
        v.setFont(new Font("Segoe UI", Font.BOLD, 13));
        v.setForeground(color);

        box.add(lbl); box.add(v);
        parent.add(box);
        parent.add(Box.createVerticalStrut(3));
        return v;
    }

    // ── Mejoras ──
    private JPanel crearPanelMejoras() {
        JPanel cont = new JPanel();
        cont.setLayout(new BoxLayout(cont, BoxLayout.Y_AXIS));
        cont.setBackground(C_BOT_BG);
        cont.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,0,1,C_BOT_BORDER),
            new EmptyBorder(10,10,10,10)));

        JLabel tit = new JLabel("MEJORAS");
        tit.setFont(new Font("Segoe UI", Font.BOLD, 9));
        tit.setForeground(new Color(112,144,192));
        tit.setAlignmentX(Component.LEFT_ALIGNMENT);
        tit.setBorder(new EmptyBorder(0,2,5,0));
        cont.add(tit);

        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        fila.setBackground(C_BOT_BG);

        panelesMejora = new JPanel[5];
        for (int i = 0; i < 5; i++) {
            panelesMejora[i] = crearTarjeta(i);
            fila.add(panelesMejora[i]);
        }

        JScrollPane sp = new JScrollPane(fila,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setBorder(null);
        sp.getViewport().setBackground(C_BOT_BG);
        sp.setBackground(C_BOT_BG);
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        cont.add(sp);
        return cont;
    }

    private JPanel crearTarjeta(int idx) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                // NO llamar super — pintamos todo nosotros para controlar el fondo exacto
                Graphics2D g2 = (Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Fondo del contenedor padre para no dejar artefactos
                g2.setColor(getParent() != null ? getParent().getBackground() : C_BOT_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Rounded rect con el color de estado actual
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean bloq  = esBloqueado(idx);
                boolean maxed = nivelMejora(idx) >= maxMejora(idx);
                Color bc = maxed ? new Color(100,200,140)
                         : bloq  ? new Color(190,200,220)
                         :         C_BOT_BORDER;
                g2.setColor(bc);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(160, BOT_ALTO - 36));
        card.setOpaque(false); // false para que nuestro paintComponent controle todo
        card.setBorder(new EmptyBorder(9, 11, 9, 11));

        // Flag para saber si el mouse está encima
        boolean[] hovering = { false };

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                hovering[0] = true;
                if (!esBloqueado(idx) && nivelMejora(idx) < maxMejora(idx))
                    card.setBackground(C_UPG_HOVER);
                JLabel lEf = (JLabel) card.getClientProperty("lblEfecto");
                if (lEf != null) lEf.setVisible(true);
            }
            @Override public void mouseExited(MouseEvent e) {
                hovering[0] = false;
                refrescarColorTarjeta(card, idx);
                JLabel lEf = (JLabel) card.getClientProperty("lblEfecto");
                if (lEf != null) lEf.setVisible(false);
            }
            @Override public void mousePressed(MouseEvent e) { onMejora(idx); }
        });

        // Guardar referencia al flag en el panel para que refrescarColorTarjeta lo respete
        card.putClientProperty("hovering", hovering);

        refrescarColorTarjeta(card, idx);
        rellenarTarjeta(card, idx);
        return card;
    }

    private void refrescarColorTarjeta(JPanel card, int idx) {
        boolean maxed = nivelMejora(idx) >= maxMejora(idx);
        boolean bloq  = esBloqueado(idx);
        boolean[] hovering = (boolean[]) card.getClientProperty("hovering");
        boolean isHover = hovering != null && hovering[0];
        if (isHover && !bloq && !maxed) card.setBackground(C_UPG_HOVER);
        else if (maxed)     card.setBackground(C_UPG_MAX_BG);
        else if (bloq)      card.setBackground(C_UPG_LOCKED);
        else                card.setBackground(C_UPG_BG);
        // Bug9: actualizar color del nombre según si tiene dinero o no
        JLabel lNom = (JLabel) card.getClientProperty("lblNombre");
        if (lNom != null)
            lNom.setForeground(bloq ? new Color(150,160,180) : C_TXT_CAJERO);
        card.repaint();
    }

    private void rellenarTarjeta(JPanel card, int idx) {
        card.removeAll();
        boolean maxed  = nivelMejora(idx) >= maxMejora(idx);
        boolean bloq   = esBloqueado(idx);
        int lv = nivelMejora(idx), mx = maxMejora(idx);

        // Fila top: nombre + costo
        JPanel top = new JPanel(new BorderLayout(4,0));
        top.setOpaque(false);
        top.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JLabel lNom = new JLabel(UPG_NOMBRES[idx]);
        lNom.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lNom.setForeground(bloq ? new Color(150,160,180) : C_TXT_CAJERO);
        // Bug9: guardar referencia al label para actualizar su color sin recrear la tarjeta
        card.putClientProperty("lblNombre", lNom);
        top.add(lNom, BorderLayout.WEST);

        JLabel lCosto;
        if (maxed) {
            lCosto = new JLabel("MAX");
            lCosto.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lCosto.setForeground(C_GREEN_UPG);
        } else {
            lCosto = new JLabel("$" + costoMejora(idx));
            lCosto.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lCosto.setForeground(bloq ? new Color(160,140,100) : C_GOLD);
        }
        top.add(lCosto, BorderLayout.EAST);
        card.add(top);
        card.add(Box.createVerticalStrut(4));

        // Descripcion base
        JLabel lDesc = new JLabel("<html><body style='width:130px'>" + UPG_DESCS[idx] + "</body></html>");
        lDesc.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lDesc.setForeground(bloq ? new Color(160,170,190) : new Color(80,112,160));
        lDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lDesc);

        // Efecto detallado — visible solo en hover
        JLabel lEfecto = new JLabel("<html><body style='width:130px'>" + UPG_EFECTOS[idx] + "</body></html>");
        lEfecto.setFont(new Font("Segoe UI", Font.ITALIC, 9));
        lEfecto.setForeground(new Color(60, 130, 80));
        lEfecto.setAlignmentX(Component.LEFT_ALIGNMENT);
        lEfecto.setVisible(false);
        card.putClientProperty("lblEfecto", lEfecto);
        card.add(lEfecto);

        card.add(Box.createVerticalGlue());

        // Dots de nivel
        JPanel dots = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        dots.setOpaque(false);
        dots.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int i = 0; i < mx; i++) {
            final boolean filled = i < lv;
            JPanel dot = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D)g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (filled) g2.setColor(maxed ? C_GREEN_UPG : new Color(80,144,224));
                    else        g2.setColor(bloq  ? new Color(190,200,220) : new Color(200,216,238));
                    g2.fillOval(0,0,10,10);
                }
            };
            dot.setPreferredSize(new Dimension(10,10));
            dot.setOpaque(false);
            dots.add(dot);
        }
        card.add(dots);
        card.revalidate();
        card.repaint();
    }

    private void onMejora(int idx) {
        boolean ok = switch (idx) {
            case 0 -> eco.mejorarCajeros();
            case 1 -> eco.mejorarFlujo();
            case 2 -> eco.mejorarVelocidad();
            case 3 -> eco.mejorarMonedas();
            case 4 -> eco.mejorarVip();
            default -> false;
        };
        if (ok) {
            sim.sincronizarCajeros();
            SwingUtilities.invokeLater(this::actualizarPanelInferior);
            // Bug8: mensaje de felicitaciones para TODAS las mejoras al llegar al máximo
            if (nivelMejora(idx) >= maxMejora(idx)) {
                String[] mensajes = {
                    "Alcanzaste la capacidad maxima de cajeros.",
                    "Alcanzaste el maximo de afluencia de clientes.",
                    "Todos los cajeros estan a maxima velocidad.",
                    "Comision maxima alcanzada.",
                    "Sala VIP al maximo nivel."
                };
                mostrarToast("Felicitaciones! " + mensajes[idx], new Color(26,154,80));
            }
        }
    }

    private int nivelMejora(int idx) {
        return switch (idx) {
            case 0 -> eco.getNivelCajeros();
            case 1 -> eco.getNivelFlujo();
            case 2 -> eco.getNivelVelocidad();
            case 3 -> eco.getNivelMonedas();
            case 4 -> eco.getNivelVip();
            default -> 0;
        };
    }
    private int maxMejora(int idx) {
        return switch (idx) {
            case 0 -> Economia.MAX_CAJEROS;
            case 1 -> Economia.MAX_FLUJO;
            case 2 -> Economia.MAX_VELOCIDAD;
            case 3 -> Economia.MAX_MONEDAS;
            case 4 -> Economia.MAX_VIP;
            default -> 1;
        };
    }
    private int costoMejora(int idx) {
        return switch (idx) {
            case 0 -> eco.costoCajeros();
            case 1 -> eco.costoFlujo();
            case 2 -> eco.costoVelocidad();
            case 3 -> eco.costoMonedas();
            case 4 -> eco.costoVip();
            default -> 999;
        };
    }
    private boolean esBloqueado(int idx) {
        if (nivelMejora(idx) >= maxMejora(idx)) return false;
        return eco.getMonedas() < costoMejora(idx);
    }

    public void actualizarPanelInferior() {
        for (int i = 0; i < 5; i++) {
            refrescarColorTarjeta(panelesMejora[i], i);
            rellenarTarjeta(panelesMejora[i], i);
        }
    }

    // ── Reproductor ──
    private JPanel crearReproductor() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(C_MUS_BG);
                g.fillRect(0,0,getWidth(),getHeight());
            }
        };
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,1,0,0,new Color(58,80,144)),
            new EmptyBorder(12,14,12,14)));

        JLabel now = new JLabel(">> NOW PLAYING");
        now.setFont(new Font("Segoe UI", Font.BOLD, 9));
        now.setForeground(C_MUS_ACCENT);
        now.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblCancion = new JLabel("...");
        lblCancion.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblCancion.setForeground(Color.WHITE);
        lblCancion.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel bots = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bots.setOpaque(false);
        bots.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnPausa = musBtn("|| Pausa");
        btnPausa.addActionListener(e -> { sonido.togglePausa(); actualizarReproductor(); });
        JButton btnSkip = musBtn(">> Skip");
        btnSkip.addActionListener(e -> sonido.siguienteCancion());
        bots.add(btnPausa); bots.add(btnSkip);

        JLabel volLbl = new JLabel("Vol.");
        volLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        volLbl.setForeground(new Color(180,200,240));
        volLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSlider sv = new JSlider(0,100,(int)(sonido.getVolumen()*100));
        sv.setOpaque(false);
        sv.setForeground(C_MUS_ACCENT);
        sv.setAlignmentX(Component.LEFT_ALIGNMENT);
        sv.setMaximumSize(new Dimension(180,20));
        sv.addChangeListener(e -> sonido.setVolumen(sv.getValue()/100f));

        p.add(now); p.add(Box.createVerticalStrut(3));
        p.add(lblCancion); p.add(Box.createVerticalStrut(8));
        p.add(bots); p.add(Box.createVerticalStrut(6));
        p.add(volLbl); p.add(Box.createVerticalStrut(3));
        p.add(sv);

        actualizarReproductor();
        return p;
    }

    private JButton musBtn(String txt) {
        JButton b = new JButton(txt);
        b.setFont(new Font("Segoe UI", Font.BOLD, 11));
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(58,80,144));
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(80, 26));
        b.setMinimumSize(new Dimension(80, 26));
        b.setMaximumSize(new Dimension(80, 26));
        return b;
    }

    private void actualizarReproductor() {
        if (lblCancion == null) return;
        lblCancion.setText(sonido.getNombreCancionActual());
        btnPausa.setText(sonido.isPausado() ? "> Play" : "|| Pausa");
    }

    // ══════════════════════════════════════════════════
    //  ANIMACION (Timer 30ms)
    // ══════════════════════════════════════════════════
    private void iniciarAnimacion() {
        animTimer = new javax.swing.Timer(30, e -> {
            actualizarPosicionesClientes();
            actualizarOverlay();
            actualizarCoinPopups();
            actualizarToasts();
            actualizarHUD();
            actualizarNotifNivel();
            // Bug12: mostrar botón de prestige cuando se alcanza la meta
            mostrarBtnPrestige = sim.isMetaAlcanzada() && !sim.isTerminado();
            if (panelesMejora != null)
                for (int i = 0; i < 5; i++) refrescarColorTarjeta(panelesMejora[i], i);
            panelBanco.repaint();
            panelDayBar.repaint();
            // Bug11: repintar glass pane para notif siempre visible
            if (getGlassPane() != null) getGlassPane().repaint();
            // Refrescar cubos del panel lateral
            if (panelLateralBancos != null && panelLateralBancos.isVisible())
                panelLateralBancos.repaint();
        });
        animTimer.start();
    }

    private void actualizarHUD() {
        long mon = eco.getMonedas();
        lblMonedasHeader.setText("$" + mon);
        lblContadorClientes.setText("Clientes Atendidos: " +
            sim.getClientesAtendidosTotal() + " / " + sim.getMetaClientes());
        long seg = (System.currentTimeMillis() - eco.getInicioJuego()) / 1000;
        String tiempoStr = fmt(seg/60) + ":" + fmt(seg%60);
        lblTimerHeader.setText("Tiempo: " + tiempoStr);
        lblGanHoy.setText("$" + eco.getGananciasHoy());
        lblMejorDia.setText("$" + eco.getMejorDia());
        lblNumCajeros.setText(String.valueOf(sim.getCajeros().size()));
        lblVelocidad.setText(String.format("%.1fs", eco.getServeBaseMs()/1000.0));
        if (lblTiempoJuego != null) lblTiempoJuego.setText(tiempoStr);
        // Actualizar billetes
        PrestigioManager pr = sim.getPrestigio();
        if (pr != null && lblBilletes != null) {
            lblBilletes.setText(pr.getBilletes() + " B");
        }
        // Actualizar mini indicador XP
        PaseBatalla pb = sim.getPaseBatalla();
        if (pb != null && lblXpNivel != null) {
            lblXpNivel.setText("NIVEL " + pb.getNivel() + (pb.esNivelMax() ? "  ★ MAX" : ""));
            if (pb.esNivelMax()) {
                lblXpProgress.setText("¡Nivel Máximo!");
            } else {
                lblXpProgress.setText(pb.getXpEnNivelActual() + " / " + pb.getXpParaSiguiente() + " XP");
            }
            if (pnlXpIndicador != null) pnlXpIndicador.repaint();
        }
    }

    private void actualizarNotifNivel() {
        if (!mostrandoNotifNivel) return;
        if (notifSubiendo) {
            notifAlpha = Math.min(1f, notifAlpha + 0.05f);
            if (notifAlpha >= 1f) {
                notifSubiendo = false;
                notifInicioMs = System.currentTimeMillis(); // empezar a contar desde aquí
            }
        } else {
            long elapsed = System.currentTimeMillis() - notifInicioMs;
            if (elapsed > 4000) {
                notifAlpha = Math.max(0f, notifAlpha - 0.04f);
                if (notifAlpha <= 0f) mostrandoNotifNivel = false;
            }
        }
        // La notif se dibuja en el glass pane, que ya se repinta en el animTimer
    }

    public void notificarSubidaNivel(int nuevoNivel) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            notifNivel          = nuevoNivel;
            notifAlpha          = 0f;
            notifSubiendo       = true;
            mostrandoNotifNivel = true;
            notifInicioMs       = System.currentTimeMillis();
        });
    }

    private void abrirVentanaPaseBatalla(boolean conAnimacion) {
        PaseBatalla pb = sim.getPaseBatalla();
        if (pb == null) return;
        mostrandoNotifNivel = false; // cerrar notif al abrir la ventana
        VentanaPaseBatalla v = new VentanaPaseBatalla(this, pb, conAnimacion);
        v.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (v.animandoSubida) v.cerrarAnimacion();
            }
        });
        v.setVisible(true);
    }

    private void actualizarPosicionesClientes() {
        List<Cajero> cajeros = sim.getCajeros();
        Set<Integer> idsActivos = new HashSet<>();

        for (int i = 0; i < cajeros.size(); i++) {
            Cajero caj = cajeros.get(i);
            int carrilY = i * CARRIL_ALTO;
            int startX  = PAD + CAJERO_W + PAD * 2;
            int ty      = carrilY + (CARRIL_ALTO - CLI_H) / 2;

            int slot = 0;
            Cliente enAt = caj.getClienteEnAtencion();

            if (enAt != null && !enAt.isDesapareciendo()) {
                idsActivos.add(enAt.getId());
                double[] pos = posiciones.computeIfAbsent(enAt.getId(),
                    k -> new double[]{ startX + 100.0, ty });
                pos[0] += (startX - pos[0]) * LERP;
                pos[1] += (ty     - pos[1]) * LERP;
                enAt.setX(pos[0]); enAt.setY(pos[1]);
                slot = 1;
            }

            List<Cliente> cola = new ArrayList<>(caj.getCola());
            for (int j = 0; j < cola.size(); j++) {
                Cliente c = cola.get(j);
                idsActivos.add(c.getId());
                int tx = startX + (j + slot) * (CLI_W + CLI_GAP);
                double[] pos = posiciones.computeIfAbsent(c.getId(),
                    k -> new double[]{ tx + 100.0, ty });
                pos[0] += (tx - pos[0]) * LERP;
                pos[1] += (ty - pos[1]) * LERP;
                c.setX(pos[0]); c.setY(pos[1]);
            }
        }
        posiciones.keySet().retainAll(idsActivos);

        // Fade-outs
        fadeOuts.forEach((id, d) -> {
            d[0] -= 0.055; // alpha
            d[1] -= 1.8;   // y offset (sube)
            if (d[0] <= 0) fadeOuts.remove(id);
        });
    }

    private void actualizarOverlay() {
        if (!mostrandoOverlayDia) return;
        if (overlaySubiendo) {
            alphaOverlay += 0.045f;
            if (alphaOverlay >= 1f) { alphaOverlay = 1f; overlaySubiendo = false; }
        } else {
            alphaOverlay -= 0.022f;
            if (alphaOverlay <= 0f) {
                alphaOverlay = 0f;
                mostrandoOverlayDia = false;
            }
        }
    }

    private void actualizarCoinPopups() {
        coinPopups.forEach(p -> {
            p[1] -= 1.4;  // sube
            p[2] -= 0.03; // fade
        });
        coinPopups.removeIf(p -> p[2] <= 0);
    }

    private void actualizarToasts() {
        toasts.forEach(t -> {
            float a = (Float)t[1];
            a -= 0.012f;
            t[1] = Math.max(0f, a);
        });
        toasts.removeIf(t -> (Float)t[1] <= 0f);
    }

    // ── Llamado desde SimulacionBanco cuando se atiende un cliente ──
    public void mostrarCoinPopup(int x, int y, int monto, boolean vip) {
        // [x, y, alpha, monto, vip(0/1)]
        coinPopups.add(new double[]{ x, y, 1.0, monto, vip ? 1 : 0 });
    }

    // ── Overlay de cambio de día ──
    public void mostrarCambioDia(int diaAnterior, int diaNuevo, long ganancias,
                                 long robado, int atrapados, int robaron) {
        textOverlay1 = "Fin del Día " + diaAnterior + "  —  $" + ganancias + " ganados";
        textOverlay2 = "Día " + diaNuevo + " comenzando...";
        // Reporte ladrones
        if (robado > 0 || atrapados > 0 || robaron > 0) {
            textOverlay3 = "Ladrones: " + atrapados + " atrapados  |  "
                         + robaron + " escaparon  |  $" + robado + " robados";
        } else {
            textOverlay3 = "Sin incidentes de seguridad hoy ✓";
        }
        alphaOverlay    = 0f;
        overlaySubiendo = true;
        mostrandoOverlayDia = true;
    }

    public void onCajeroAgregado(Cajero c) {
        SwingUtilities.invokeLater(() -> {
            // Solo revalidar el panel sin modificar el tamaño de la ventana
            panelBanco.revalidate();
            panelBanco.repaint();
        });
    }

    public void onNuevoDia() {
        SwingUtilities.invokeLater(this::actualizarPanelInferior);
    }

    public void mostrarToast(String mensaje, Color color) {
        toasts.add(new Object[]{ mensaje, 1.0f, color, 0.0f });
    }

    public void mostrarBonusMeta(long bonus) {
        mostrarToast("\u00a1Superaste la meta del dia! +" + bonus + " monedas bonus", new Color(26,154,80));
    }

    public void mostrarAvisoLadron() {
        JOptionPane.showMessageDialog(this,
            "<html><b>\u26a0\ufe0f \u00a1Alerta de seguridad!</b><br><br>" +
            "Un <b>ladron</b> se ha colado entre los clientes.<br>" +
            "Puedes reconocerlo porque su nombre aparece en <b>rojo oscuro</b>.<br><br>" +
            "Si llega a ser atendido por un cajero, <b>te robara $50</b>.<br>" +
            "Para eliminarlo: <b>haz clic sobre el</b> y el guardia lo sacara.<br><br>" +
            "<i>\u00a1Mantente atento! Seguiran apareciendo de vez en cuando.</i></html>",
            "\u00a1Ladron detectado!", JOptionPane.WARNING_MESSAGE);
    }

    public void mostrarRoboLadron(int x, int y, long monto) {
        // vip=2 → rojo (robo)
        coinPopups.add(new double[]{ x, y, 1.4, -monto, 2 });
    }

    public void mostrarRecompensaLadron(int x, int y, long monto) {
        // vip=3 → azul/verde brillante (recompensa por atrapar)
        coinPopups.add(new double[]{ x, y, 1.4, monto, 3 });
    }

    // ── Llamado desde clienteAtendido para marcar fade ──
    public void iniciarFadeCliente(Cliente cl, int cajeroIdx) {
        List<Cajero> cajeros = sim.getCajeros();
        int carrilY = cajeroIdx * CARRIL_ALTO;
        int startX  = PAD + CAJERO_W + PAD * 2;
        int ty      = carrilY + (CARRIL_ALTO - CLI_H) / 2;
        double[] pos = posiciones.getOrDefault(cl.getId(), new double[]{startX, ty});
        // [alpha, yOffset, nombre, x, y, vip, serving]
        fadeOuts.put(cl.getId(), new double[]{
            1.0, pos[1], 0, pos[0], pos[1], cl.isVip() ? 1 : 0, 1
        });
        posiciones.remove(cl.getId());

        // Coin popup sobre el cliente
        int mx = (int)pos[0] + CLI_W/2;
        int my = (int)pos[1];
        SwingUtilities.invokeLater(() -> mostrarCoinPopup(mx, my, eco.getMonedasPorCliente(cl.isVip()), cl.isVip()));
    }

    // ══════════════════════════════════════════════════
    //  PANEL BANCO
    // ══════════════════════════════════════════════════
    class PanelBanco extends JPanel {
        PanelBanco() {
            setBackground(C_FONDO);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    sim.intentarAtraparLadron(e.getX(), e.getY());
                    sim.atenderManual(e.getX(), e.getY());
                }
            });
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(0, Math.max(sim.getCajeros().size() * CARRIL_ALTO, 10));
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            List<Cajero> cajeros = sim.getCajeros();
            for (int i = 0; i < cajeros.size(); i++)
                dibujarCarril(g2, cajeros.get(i), i * CARRIL_ALTO, i);

            // Fade-outs
            fadeOuts.forEach((id, d) -> {
                if (d[0] <= 0) return;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)Math.max(0,d[0])));
                dibujarCuadroCliente(g2, "...", (int)d[3], (int)d[1], true, d[5]>0);
                g2.setComposite(AlphaComposite.SrcOver);
            });

            // Coin popups (p[4]: 0=normal, 1=VIP, 2=robo)
            coinPopups.forEach(p -> {
                if (p[2] <= 0) return;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)Math.min(1, p[2])));
                boolean robo  = (p[4] == 2);
                boolean vip   = (p[4] == 1);
                boolean recom = (p[4] == 3);
                String txt = robo  ? "-" + (int)Math.abs(p[3]) + " ROBADO!"
                           : recom ? "+" + (int)p[3] + " ATRAPADO!"
                           : "+"   + (int)p[3] + (vip ? " VIP" : "");
                g2.setFont(new Font("Segoe UI", Font.BOLD, (robo||recom) ? 15 : 13));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (int)p[0] - fm.stringWidth(txt)/2;
                int ty = (int)p[1];
                // Sombra
                g2.setColor(new Color(0,0,0,80));
                g2.drawString(txt, tx+1, ty+1);
                // Color según tipo
                Color textColor = robo  ? new Color(255,40,40)
                                : recom ? new Color(40,210,120)
                                : vip   ? new Color(210,150,0)
                                :         new Color(30,150,60);
                g2.setColor(textColor);
                g2.drawString(txt, tx, ty);
                g2.setComposite(AlphaComposite.SrcOver);
            });

            // Toasts (mensajes flotantes)
            int toastY = 60;
            for (Object[] t : toasts) {
                float alpha = (Float)t[1];
                if (alpha <= 0) continue;
                String msg = (String)t[0];
                Color  col = (Color)t[2];
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(msg) + 24;
                int th = 34;
                int tx2 = (getWidth() - tw) / 2;
                int ty2 = toastY;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.88f));
                g2.setColor(new Color(20,20,40));
                g2.fillRoundRect(tx2, ty2, tw, th, 12, 12);
                g2.setColor(col);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(tx2, ty2, tw, th, 12, 12);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(Color.WHITE);
                g2.drawString(msg, tx2+12, ty2 + th/2 + fm.getAscent()/2 - 2);
                g2.setComposite(AlphaComposite.SrcOver);
                toastY += th + 6;
            }

            // Overlay cambio de dia
            if (mostrandoOverlayDia && alphaOverlay > 0) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaOverlay * 0.78f));
                g2.setColor(new Color(10, 20, 60));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaOverlay));

                int cy = getHeight() / 2;

                // Línea 1: ganancias del día
                g2.setFont(new Font("Segoe UI", Font.BOLD, 26));
                FontMetrics fm = g2.getFontMetrics();
                String t1 = textOverlay1;
                g2.setColor(new Color(255,230,80));
                g2.drawString(t1, (getWidth()-fm.stringWidth(t1))/2, cy - 30);

                // Línea 2: reporte ladrones
                String t3 = textOverlay3;
                if (t3 != null && !t3.isEmpty()) {
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    fm = g2.getFontMetrics();
                    boolean haRobado = t3.contains("$") && !t3.contains("$0");
                    g2.setColor(haRobado ? new Color(255,120,120) : new Color(100,240,160));
                    g2.drawString(t3, (getWidth()-fm.stringWidth(t3))/2, cy + 6);
                }

                // Línea 3: inicio nuevo día
                g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                fm = g2.getFontMetrics();
                String t2 = textOverlay2;
                g2.setColor(new Color(180,210,255));
                g2.drawString(t2, (getWidth()-fm.stringWidth(t2))/2, cy + 36);

                g2.setComposite(AlphaComposite.SrcOver);
            }

            if (sim.isTerminado()) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                g2.setColor(Color.BLACK);
                g2.fillRect(0,0,getWidth(),getHeight());
                g2.setComposite(AlphaComposite.SrcOver);
            }
        }

        private void dibujarCarril(Graphics2D g2, Cajero caj, int y, int idx) {
            g2.setColor(idx % 2 == 0 ? C_CARRIL_A : C_CARRIL_B);
            g2.fillRect(0, y, getWidth(), CARRIL_ALTO);
            g2.setColor(C_SEP);
            g2.drawLine(0, y+CARRIL_ALTO-1, getWidth(), y+CARRIL_ALTO-1);

            dibujarCajeroBox(g2, caj, PAD, y + (CARRIL_ALTO - CAJERO_H)/2);

            Cliente enAt = caj.getClienteEnAtencion();
            if (enAt != null && !enAt.isDesapareciendo() && posiciones.containsKey(enAt.getId())) {
                if (enAt instanceof Ladron l) dibujarCuadroLadron(g2, l, (int)enAt.getX(), (int)enAt.getY(), true);
                else dibujarCuadroCliente(g2, enAt.getNombre(), (int)enAt.getX(), (int)enAt.getY(), true, enAt.isVip());
            }

            for (Cliente c : new ArrayList<>(caj.getCola())) {
                if (!posiciones.containsKey(c.getId())) continue;
                if (c instanceof Ladron l) dibujarCuadroLadron(g2, l, (int)c.getX(), (int)c.getY(), false);
                else dibujarCuadroCliente(g2, c.getNombre(), (int)c.getX(), (int)c.getY(), false, c.isVip());
            }
        }

        /** Dibuja el cuadro especial de un ladrón */
        private void dibujarCuadroLadron(Graphics2D g2, Ladron l, int x, int y, boolean srv) {
            // Fondo rojo oscuro pulsante
            Color bg     = new Color(180, 30, 30);
            Color borde  = new Color(230, 60, 60);

            // Sombra
            g2.setColor(new Color(0,0,0,30));
            g2.fillRoundRect(x+2, y+2, CLI_W, CLI_H, 10, 10);

            g2.setColor(bg);
            g2.fillRoundRect(x, y, CLI_W, CLI_H, 10, 10);
            g2.setColor(borde);
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRoundRect(x, y, CLI_W, CLI_H, 10, 10);
            g2.setStroke(new BasicStroke(1f));

            // Icono
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.setColor(new Color(255,80,80));
            FontMetrics fmv = g2.getFontMetrics();
            String icon = "!! LADRON";
            g2.drawString(icon, x+(CLI_W-fmv.stringWidth(icon))/2, y+13);

            // Nombre
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.setColor(new Color(255,200,200));
            FontMetrics fm = g2.getFontMetrics();
            String txt = l.getNombre();
            g2.drawString(txt, x+(CLI_W-fm.stringWidth(txt))/2, y+CLI_H/2+fm.getAscent()/2+3);

            // Animación policia encima
            if (l.isMostrandoPolicia() && l.getAlphaPolicia() > 0) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, l.getAlphaPolicia()));
                g2.setColor(new Color(30,100,220));
                g2.fillRoundRect(x-4, y-4, CLI_W+8, CLI_H+8, 12, 12);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                FontMetrics fmp = g2.getFontMetrics();
                String ptxt = "POLICIA";
                g2.drawString(ptxt, x+(CLI_W-fmp.stringWidth(ptxt))/2, y+CLI_H/2+4);
                g2.setComposite(AlphaComposite.SrcOver);
                // fade
                l.setAlphaPolicia(l.getAlphaPolicia() - 0.04f);
            }
        }

        private void dibujarCajeroBox(Graphics2D g2, Cajero caj, int x, int y) {
            Color bg = !caj.isAbierto() ? C_CAJERO_CLOSE
                     : caj.isRapido()  ? C_CAJERO_FAST
                     :                   C_CAJERO_OPEN;

            // Sombra
            g2.setColor(new Color(0,0,0,28));
            g2.fillRoundRect(x+3, y+3, CAJERO_W, CAJERO_H, 14, 14);

            // Cuerpo
            g2.setColor(bg);
            g2.fillRoundRect(x, y, CAJERO_W, CAJERO_H, 14, 14);

            // Borde
            g2.setColor(caj.isAbierto() ? new Color(100,160,220) : new Color(180,40,40));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x, y, CAJERO_W, CAJERO_H, 14, 14);
            g2.setStroke(new BasicStroke(1f));

            // Nombre
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.setColor(caj.isAbierto() ? C_TXT_CAJERO : Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            String nom = caj.getNombre();
            g2.drawString(nom, x+(CAJERO_W-fm.stringWidth(nom))/2, y+CAJERO_H/2+2);

            // Estado
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            String est = !caj.isAbierto() ? "CERRADO" : caj.isRapido() ? "RAPIDO" : "ABIERTO";
            g2.setColor(caj.isAbierto() ? new Color(60,100,180,160) : new Color(255,255,255,190));
            fm = g2.getFontMetrics();
            g2.drawString(est, x+(CAJERO_W-fm.stringWidth(est))/2, y+CAJERO_H/2+15);

            // Barra de progreso de atención
            double prog = caj.getProgresoAtencion();
            if (prog > 0.001) {
                int bx = x+6, by = y+CAJERO_H-8, bw = CAJERO_W-12;
                g2.setColor(new Color(0,0,0,35));
                g2.fillRoundRect(bx, by, bw, 5, 3, 3);
                g2.setColor(caj.isRapido() ? new Color(30,160,80) : new Color(60,120,220));
                g2.fillRoundRect(bx, by, (int)(bw*prog), 5, 3, 3);
            }
        }

        private void dibujarCuadroCliente(Graphics2D g2, String nombre, int x, int y, boolean srv, boolean vip) {
            Color bg    = srv ? (vip ? C_CLIENTE_VSRV : C_CLIENTE_SRV) : (vip ? C_CLIENTE_VIP : C_CLIENTE);
            Color borde = srv ? (vip ? new Color(40,140,70) : new Color(60,160,90))
                              : (vip ? new Color(200,150,10) : new Color(200,150,80));

            g2.setColor(new Color(0,0,0,20));
            g2.fillRoundRect(x+2, y+2, CLI_W, CLI_H, 10, 10);

            g2.setColor(bg);
            g2.fillRoundRect(x, y, CLI_W, CLI_H, 10, 10);

            g2.setColor(borde);
            g2.setStroke(new BasicStroke(srv ? 2.2f : 1.5f));
            g2.drawRoundRect(x, y, CLI_W, CLI_H, 10, 10);
            g2.setStroke(new BasicStroke(1f));

            if (vip) {
                g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
                g2.setColor(srv ? new Color(30,120,60) : new Color(160,100,0));
                FontMetrics fmv = g2.getFontMetrics();
                String vt = "* VIP";
                g2.drawString(vt, x+(CLI_W-fmv.stringWidth(vt))/2, y+11);
            }

            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.setColor(srv ? new Color(20,100,40) : C_TXT_CLIENTE);
            FontMetrics fm = g2.getFontMetrics();
            String txt = nombre;
            while (txt.length() > 3 && fm.stringWidth(txt) > CLI_W - 8)
                txt = txt.substring(0, txt.length()-1);
            if (!txt.equals(nombre)) txt += "..";
            g2.drawString(txt, x+(CLI_W-fm.stringWidth(txt))/2,
                y + (vip ? CLI_H/2+fm.getAscent()/2+3 : CLI_H/2+fm.getAscent()/2-2));
        }
    }

    // ══════════════════════════════════════════════════
    //  PANEL DAY BAR
    // ══════════════════════════════════════════════════
    class PanelDayBar extends JPanel {
        PanelDayBar() {
            setBackground(new Color(221, 232, 255));
            setBorder(BorderFactory.createMatteBorder(0,0,1,0,C_SEP));
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            String[] dias = {"Lunes","Martes","Miercoles","Jueves","Viernes","Sabado","Domingo"};
            String diaStr = dias[(eco.getDia()-1)%7] + " - Dia " + eco.getDia();

            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2.setColor(new Color(30,60,120));
            g2.drawString(diaStr, 18, 21);

            int atHoy  = sim.getClientesAtendidosHoy();
            int metaHoy = eco.getClientesObjetivoDia();
            boolean metaAlcanzada = atHoy >= metaHoy;
            String cl = "Meta dia: " + atHoy + " / " + metaHoy + (metaAlcanzada ? " \u2713" : "");
            g2.setFont(new Font("Segoe UI", metaAlcanzada ? Font.BOLD : Font.PLAIN, 11));
            g2.setColor(metaAlcanzada ? new Color(26,154,80) : new Color(80,112,160));
            g2.drawString(cl, 180, 21);

            long elapsed = System.currentTimeMillis() - eco.getInicioDia();
            double pct   = Math.min(1.0, (double)elapsed / sim.getDuracionDia());
            int bx = 340, bw = getWidth() - bx - 100, bh = 8, by = (DAYBAR_ALTO-bh)/2;
            if (bw > 20) {
                g2.setColor(new Color(80,120,200,40));
                g2.fillRoundRect(bx, by, bw, bh, 5, 5);
                g2.setColor(C_HEADER1);
                g2.fillRoundRect(bx, by, (int)(bw*pct), bh, 5, 5);
            }

            long rem = Math.max(0, (sim.getDuracionDia()-elapsed)/1000);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.setColor(new Color(80,112,160));
            g2.drawString(rem + "s", getWidth()-50, 21);
        }
    }

    // ══════════════════════════════════════════════════
    //  PRESTIGE
    // ══════════════════════════════════════════════════

    /** Llamado por SimulacionBanco cuando se llega a 200 clientes */
    public void ofrecerPrestigio(SimulacionBanco simQuePrestigia) {
        SwingUtilities.invokeLater(() -> {
            PrestigioManager pr = simQuePrestigia.getPrestigio();
            VentanaPrestigio vp = new VentanaPrestigio(this, pr, simQuePrestigia.getClientesAtendidosTotal());
            vp.setVisible(true);

            if (vp.getResultado() == VentanaPrestigio.Resultado.PRESTIGE) {
                pr.agregarBilletes(pr.billetesDelPrestige());
                pr.incrementarPrestigios();
                activarUIPrestigio();
                if (onRefrescarCubos != null) onRefrescarCubos.run();
                // Bug17: notificar a Main qué banco debe reiniciarse
                if (onPrestigioConBanco != null) onPrestigioConBanco.accept(simQuePrestigia);
            } else {
                mostrarToast("Continuas jugando sin limite. Usa el boton de prestige cuando quieras.", new Color(60,120,220));
                if (!animTimer.isRunning()) animTimer.start();
            }
        });
    }

    // Mantener el método sin parámetro por compatibilidad con prestigiarAhora()
    public void ofrecerPrestigio() { ofrecerPrestigio(sim); }

    public interface SimConsumer { void accept(SimulacionBanco s); }
    private SimConsumer onPrestigioConBanco;
    public void setOnPrestigioConBanco(SimConsumer c) { onPrestigioConBanco = c; }

    private Runnable onRefrescarCubos;
    public void setOnRefrescarCubos(Runnable r) { onRefrescarCubos = r; }

    /** Muestra los elementos de prestige en la UI (billetes, botón bancos) */
    public void activarUIPrestigioPublico() { activarUIPrestigio(); }

    private void activarUIPrestigio() {
        if (panelLateralBancos != null) panelLateralBancos.setVisible(true);
        buscarYMostrarBilletes(getContentPane());
        revalidate(); repaint();
    }

    private void buscarYMostrarBilletes(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JPanel jp) {
                Object pb = jp.getClientProperty("panelBilletes");
                if (pb instanceof JPanel panelB) { panelB.setVisible(true); return; }
                buscarYMostrarBilletes(jp);
            }
        }
    }

    /** Reinicia la simulación del banco actual para el nuevo prestige */
    private void abrirArbolHabilidades() {
        PrestigioManager pr = sim.getPrestigio();
        if (pr == null) return;
        VentanaArbolHabilidades v = new VentanaArbolHabilidades(this, pr,
            () -> SwingUtilities.invokeLater(this::actualizarPanelInferior));
        v.setVisible(true);
    }

    private void abrirVentanaBancos() {
        if (ventanaBancos != null && ventanaBancos.isVisible()) {
            ventanaBancos.toFront(); return;
        }
        PrestigioManager pr = sim.getPrestigio();
        if (pr == null) return;
        // Obtener lista de sims y ecos del Main via callback
        if (onAbrirBancos != null) onAbrirBancos.run();
    }

    private Runnable onAbrirBancos;
    public void setOnAbrirBancos(Runnable r) { this.onAbrirBancos = r; }

    public void mostrarVentanaBancos(java.util.List<SimulacionBanco> sims,
                                     java.util.List<Economia> ecos,
                                     PrestigioManager pr,
                                     Runnable onCompraBanco) {
        if (ventanaBancos != null) ventanaBancos.detenerRefresh();
        ventanaBancos = new VentanaBancos(this, sims, ecos, pr, () -> {
            if (onCompraBanco != null) onCompraBanco.run();
            SwingUtilities.invokeLater(() -> mostrarToast("Nuevo banco comprado!", new Color(180,120,255)));
        });
        ventanaBancos.setVisible(true);
    }

    // ══════════════════════════════════════════════════
    //  GLASS PANE HELPERS
    // ══════════════════════════════════════════════════

    /** Bug11: dibuja la notificación de nivel en el glass pane (siempre visible) */
    private void dibujarNotifNivel(Graphics2D g2, int W, int H) {
        float a = notifAlpha;
        int nw = 340, nh = 90;
        int nx = W - nw - 16, ny = 16;

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a * 0.4f));
        g2.setColor(Color.BLACK);
        g2.fillRoundRect(nx+4, ny+4, nw, nh, 16, 16);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a * 0.95f));
        g2.setColor(new Color(15, 25, 55));
        g2.fillRoundRect(nx, ny, nw, nh, 16, 16);
        g2.setColor(new Color(30, 120, 255));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(nx, ny, nw, nh, 16, 16);
        g2.setStroke(new BasicStroke(1f));

        g2.setColor(new Color(255, 200, 50));
        g2.fillRoundRect(nx, ny, 6, nh, 6, 6);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.setColor(new Color(80, 170, 255));
        g2.drawString("PASE DE BATALLA", nx+18, ny+20);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
        g2.setColor(new Color(255, 200, 50));
        g2.drawString("¡Subiste al Nivel " + notifNivel + "!", nx+18, ny+46);

        PaseBatalla pb = sim.getPaseBatalla();
        if (pb != null) {
            PaseBatalla.Recompensa r = pb.getRecompensa(notifNivel);
            if (r != null) {
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g2.setColor(new Color(200, 255, 200));
                g2.drawString("+ " + r.descripcion, nx+18, ny+66);
            }
        }
        g2.setColor(new Color(30, 120, 255, 200));
        g2.fillRoundRect(nx+nw-68, ny+nh-30, 58, 20, 8, 8);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.setColor(Color.WHITE);
        g2.drawString("Ver ->", nx+nw-56, ny+nh-16);
        g2.setComposite(AlphaComposite.SrcOver);
    }

    /** Bug12: botón de prestige manual visible en esquina tras alcanzar 200 clientes */
    private void dibujarBotonPrestige(Graphics2D g2, int W, int H) {
        int bw = 190, bh = 36;
        int bx = W - bw - 16, by = H - bh - 16;

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.92f));
        g2.setColor(new Color(60, 20, 100));
        g2.fillRoundRect(bx, by, bw, bh, 10, 10);
        g2.setColor(new Color(180, 120, 255));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(bx, by, bw, bh, 10, 10);
        g2.setStroke(new BasicStroke(1f));

        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        String txt = "Hacer Prestige ahora";
        g2.drawString(txt, bx + (bw - fm.stringWidth(txt))/2, by + bh/2 + fm.getAscent()/2 - 2);
        g2.setComposite(AlphaComposite.SrcOver);
    }

    // ══════════════════════════════════════════════════
    //  FIN DEL JUEGO
    // ══════════════════════════════════════════════════
    public void mostrarFin() {
        SwingUtilities.invokeLater(() -> {
            animTimer.stop();
            repaint();

            long seg = (System.currentTimeMillis()-eco.getInicioJuego())/1000;
            JDialog d = new JDialog(this, "Banco Exitoso!", true);
            d.setSize(440, 280);
            d.setLocationRelativeTo(this);

            JPanel pan = new JPanel(new BorderLayout(0,10));
            pan.setBackground(C_FONDO);
            pan.setBorder(new EmptyBorder(24,24,24,24));

            JLabel tit = new JLabel(sim.getMetaClientes() + " Clientes Atendidos!", SwingConstants.CENTER);
            tit.setFont(new Font("Segoe UI", Font.BOLD, 22));
            tit.setForeground(C_TXT_CAJERO);

            JPanel stats = new JPanel(new GridLayout(1,3,12,0));
            stats.setBackground(C_FONDO);
            stats.add(finStat("Monedas totales","$"+eco.getTotalGanado()));
            stats.add(finStat("Tiempo total", fmt(seg/60)+":"+fmt(seg%60)));
            stats.add(finStat("Mejor dia","$"+eco.getMejorDia()));

            JButton ok = new JButton("Cerrar");
            ok.setFont(new Font("Segoe UI", Font.BOLD, 14));
            ok.setBackground(C_HEADER1);
            ok.setForeground(Color.WHITE);
            ok.setBorderPainted(false); ok.setFocusPainted(false);
            ok.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            ok.addActionListener(e -> System.exit(0));

            JPanel bot = new JPanel(new FlowLayout());
            bot.setBackground(C_FONDO);
            bot.add(ok);

            pan.add(tit, BorderLayout.NORTH);
            pan.add(stats, BorderLayout.CENTER);
            pan.add(bot, BorderLayout.SOUTH);
            d.add(pan);
            d.setVisible(true);
        });
    }

    private JPanel finStat(String label, String val) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(225,235,255));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BOT_BORDER,1),
            new EmptyBorder(10,12,10,12)));
        JLabel l = new JLabel(label); l.setFont(new Font("Segoe UI",Font.PLAIN,11)); l.setForeground(new Color(80,100,150)); l.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel v = new JLabel(val);   v.setFont(new Font("Segoe UI",Font.BOLD,20));  v.setForeground(C_TXT_CAJERO);          v.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(l); p.add(v);
        return p;
    }

    private String fmt(long n) { return String.format("%02d", n); }
}
