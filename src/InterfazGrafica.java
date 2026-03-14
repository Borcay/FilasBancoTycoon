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
    private static final int BOT_ALTO    = 185;
    private static final int CAJERO_W   = 90;
    private static final int CAJERO_H   = 56;
    private static final int CLI_W      = 72;
    private static final int CLI_H      = 46;
    private static final int CLI_GAP    = 8;
    private static final int PAD        = 14;
    private static final double LERP    = 0.14;

    private final SimulacionBanco sim;
    private final Economia eco;
    private final SonidoManager sonido;

    // ── Animación de clientes ──
    // posiciones interpoladas: clienteId -> [x, y]
    private final ConcurrentHashMap<Integer, double[]> posiciones = new ConcurrentHashMap<>();

    // clientes en fade-out: clienteId -> [alpha, yOffset, nombre, x, y, vip(0/1), srv(0/1)]
    private final ConcurrentHashMap<Integer, double[]> fadeOuts = new ConcurrentHashMap<>();

    // ── Popups de monedas: [x, y, alpha, valor, vip] ──
    private final CopyOnWriteArrayList<double[]> coinPopups = new CopyOnWriteArrayList<>();

    // ── Overlay de cambio de día ──
    private volatile boolean mostrandoOverlayDia = false;
    private volatile String  textOverlay1 = "";
    private volatile String  textOverlay2 = "";
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
    private JLabel lblGanHoy, lblMejorDia, lblNumCajeros, lblVelocidad;

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

    // ── Reproductor ──
    private JLabel  lblCancion;
    private JButton btnPausa;

    public InterfazGrafica(SimulacionBanco sim, Economia eco) {
        this.sim    = sim;
        this.eco    = eco;
        this.sonido = sim.getSonido();
        configurarVentana();
        construirUI();
        iniciarAnimacion();
        sonido.setOnCancionCambia(() -> SwingUtilities.invokeLater(this::actualizarReproductor));
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
        add(crearHeader(),         BorderLayout.NORTH);
        add(crearCentro(),         BorderLayout.CENTER);
        add(crearPanelInferior(),  BorderLayout.SOUTH);
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

        JPanel centroWrap = new JPanel(new GridBagLayout());
        centroWrap.setOpaque(false);
        centroWrap.add(centroMon);
        p.add(centroWrap, BorderLayout.CENTER);

        // Derecha: timer + leyenda
        JPanel der = new JPanel();
        der.setLayout(new BoxLayout(der, BoxLayout.Y_AXIS));
        der.setOpaque(false);
        der.setBorder(new EmptyBorder(10, 8, 8, 20));

        lblTimerHeader = new JLabel("Tiempo: 00:00");
        lblTimerHeader.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblTimerHeader.setForeground(new Color(255,255,255,210));
        lblTimerHeader.setAlignmentX(Component.RIGHT_ALIGNMENT);
        der.add(lblTimerHeader);

        der.add(Box.createVerticalStrut(6));

        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        leyenda.setOpaque(false);
        leyenda.add(dotLeyenda(C_CAJERO_OPEN,  "Abierto"));
        leyenda.add(dotLeyenda(C_CAJERO_CLOSE, "Cerrado"));
        leyenda.add(dotLeyenda(C_CAJERO_FAST,  "Rapido"));
        leyenda.add(dotLeyenda(C_CLIENTE_VIP,  "VIP"));
        der.add(leyenda);

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
        JScrollPane scroll = new JScrollPane(panelBanco,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(C_FONDO);
        c.add(scroll, BorderLayout.CENTER);
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
        stats.setPreferredSize(new Dimension(152, BOT_ALTO));
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
            new EmptyBorder(10,12,10,12)));

        JLabel tit = new JLabel("ESTADISTICAS");
        tit.setFont(new Font("Segoe UI", Font.BOLD, 9));
        tit.setForeground(new Color(112,144,192));
        tit.setAlignmentX(Component.LEFT_ALIGNMENT);
        tit.setBorder(new EmptyBorder(0,0,5,0));
        p.add(tit);

        lblGanHoy    = mkStatBox(p, "Ganancias hoy",   "$0",   C_GOLD);
        lblMejorDia  = mkStatBox(p, "Mejor dia",       "$0",   new Color(48,96,192));
        lblNumCajeros= mkStatBox(p, "Cajeros activos", "1",    C_GREEN_UPG);
        lblVelocidad = mkStatBox(p, "Tiempo/cliente",  "4.0s", new Color(180,40,40));
        return p;
    }

    private JLabel mkStatBox(JPanel parent, String label, String val, Color color) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(Color.WHITE);
        box.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BOT_BORDER, 1),
            new EmptyBorder(4,8,4,8)));
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JLabel lbl = new JLabel(label.toUpperCase());
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        lbl.setForeground(new Color(112,144,192));

        JLabel v = new JLabel(val);
        v.setFont(new Font("Segoe UI", Font.BOLD, 16));
        v.setForeground(color);

        box.add(lbl); box.add(v);
        parent.add(box);
        parent.add(Box.createVerticalStrut(4));
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
                Graphics2D g2 = (Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
            }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean bloq   = esBloqueado(idx);
                boolean maxed  = nivelMejora(idx) >= maxMejora(idx);
                Color bc = maxed  ? new Color(100,200,140)
                         : bloq   ? new Color(190,200,220)
                         :          C_BOT_BORDER;
                g2.setColor(bc);
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(160, BOT_ALTO - 36));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(9,11,9,11));

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (!esBloqueado(idx) && nivelMejora(idx) < maxMejora(idx))
                    card.setBackground(C_UPG_HOVER);
                else
                    card.setBackground(nivelMejora(idx) >= maxMejora(idx) ? C_UPG_MAX_BG
                                     : esBloqueado(idx) ? C_UPG_LOCKED : C_UPG_BG);
            }
            @Override public void mouseExited(MouseEvent e) { refrescarColorTarjeta(card, idx); }
            @Override public void mouseClicked(MouseEvent e) { onMejora(idx); }
        });

        refrescarColorTarjeta(card, idx);
        rellenarTarjeta(card, idx);
        return card;
    }

    private void refrescarColorTarjeta(JPanel card, int idx) {
        boolean maxed = nivelMejora(idx) >= maxMejora(idx);
        boolean bloq  = esBloqueado(idx);
        if (maxed)     card.setBackground(C_UPG_MAX_BG);
        else if (bloq) card.setBackground(C_UPG_LOCKED);
        else           card.setBackground(C_UPG_BG);
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

        // Descripcion
        JLabel lDesc = new JLabel("<html><body style='width:130px'>" + UPG_DESCS[idx] + "</body></html>");
        lDesc.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lDesc.setForeground(bloq ? new Color(160,170,190) : new Color(80,112,160));
        lDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lDesc);

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
            actualizarHUD();
            // Refrescar colores de mejoras cada tick (refleja cambios de monedas)
            if (panelesMejora != null)
                for (int i = 0; i < 5; i++) refrescarColorTarjeta(panelesMejora[i], i);
            panelBanco.repaint();
            panelDayBar.repaint();
        });
        animTimer.start();
    }

    private void actualizarHUD() {
        long mon = eco.getMonedas();
        lblMonedasHeader.setText("$" + mon);
        lblContadorClientes.setText("Clientes Atendidos: " +
            sim.getClientesAtendidosTotal() + " / " + sim.getMetaClientes());
        long seg = (System.currentTimeMillis() - eco.getInicioJuego()) / 1000;
        lblTimerHeader.setText("Tiempo: " + fmt(seg/60) + ":" + fmt(seg%60));
        lblGanHoy.setText("$" + eco.getGananciasHoy());
        lblMejorDia.setText("$" + eco.getMejorDia());
        lblNumCajeros.setText(String.valueOf(sim.getCajeros().size()));
        lblVelocidad.setText(String.format("%.1fs", eco.getServeBaseMs()/1000.0));
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

    // ── Llamado desde SimulacionBanco cuando se atiende un cliente ──
    public void mostrarCoinPopup(int x, int y, int monto, boolean vip) {
        // [x, y, alpha, monto, vip(0/1)]
        coinPopups.add(new double[]{ x, y, 1.0, monto, vip ? 1 : 0 });
    }

    // ── Overlay de cambio de día ──
    public void mostrarCambioDia(int diaAnterior, int diaNuevo, long ganancias) {
        textOverlay1 = "Fin del Dia " + diaAnterior + "  —  $" + ganancias + " ganados";
        textOverlay2 = "Inicio del Dia " + diaNuevo;
        alphaOverlay    = 0f;
        overlaySubiendo = true;
        mostrandoOverlayDia = true;
    }

    public void onCajeroAgregado(Cajero c) {
        SwingUtilities.invokeLater(() -> {
            int h = HEADER_ALTO + DAYBAR_ALTO + sim.getCajeros().size() * CARRIL_ALTO + BOT_ALTO + 38;
            setSize(getWidth(), Math.min(h, Toolkit.getDefaultToolkit().getScreenSize().height - 60));
            panelBanco.revalidate();
        });
    }

    public void onNuevoDia() {
        SwingUtilities.invokeLater(this::actualizarPanelInferior);
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
        PanelBanco() { setBackground(C_FONDO); }

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

            // Coin popups
            coinPopups.forEach(p -> {
                if (p[2] <= 0) return;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)Math.min(1, p[2])));
                String txt = "+" + (int)p[3] + (p[4]>0 ? " VIP" : "");
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (int)p[0] - fm.stringWidth(txt)/2;
                int ty = (int)p[1];
                // Sombra del texto
                g2.setColor(new Color(0,0,0,60));
                g2.drawString(txt, tx+1, ty+1);
                // Texto
                g2.setColor(p[4]>0 ? new Color(180,120,0) : new Color(30,130,50));
                g2.drawString(txt, tx, ty);
                g2.setComposite(AlphaComposite.SrcOver);
            });

            // Overlay cambio de dia
            if (mostrandoOverlayDia && alphaOverlay > 0) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaOverlay * 0.72f));
                g2.setColor(new Color(20, 40, 100));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaOverlay));

                g2.setFont(new Font("Segoe UI", Font.BOLD, 28));
                FontMetrics fm = g2.getFontMetrics();
                String t1 = textOverlay1;
                g2.setColor(new Color(255,230,80));
                g2.drawString(t1, (getWidth()-fm.stringWidth(t1))/2, getHeight()/2 - 14);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
                fm = g2.getFontMetrics();
                String t2 = textOverlay2;
                g2.setColor(Color.WHITE);
                g2.drawString(t2, (getWidth()-fm.stringWidth(t2))/2, getHeight()/2 + 20);

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

            // Solo dibujar clientes cuya posición ya fue calculada (evita parpadeo)
            Cliente enAt = caj.getClienteEnAtencion();
            if (enAt != null && !enAt.isDesapareciendo() && posiciones.containsKey(enAt.getId()))
                dibujarCuadroCliente(g2, enAt.getNombre(), (int)enAt.getX(), (int)enAt.getY(), true, enAt.isVip());

            for (Cliente c : new ArrayList<>(caj.getCola())) {
                if (posiciones.containsKey(c.getId()))
                    dibujarCuadroCliente(g2, c.getNombre(), (int)c.getX(), (int)c.getY(), false, c.isVip());
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

            String cl = sim.getClientesAtendidosHoy() + " / " + eco.getClientesObjetivoDia() + " hoy";
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.setColor(new Color(80,112,160));
            g2.drawString(cl, 205, 21);

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
            g2.drawString(rem + "s", getWidth()-90, 21);

            String meta = "Meta: " + sim.getClientesAtendidosTotal() + "/" + sim.getMetaClientes();
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(meta, getWidth()-fm.stringWidth(meta)-6, 21);
        }
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
            stats.add(finStat("Monedas totales","$"+eco.getMonedas()));
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
