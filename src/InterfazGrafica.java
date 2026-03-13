import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class InterfazGrafica extends JFrame {

    private static final Color COLOR_CAJERO_ABIERTO = new Color(180, 220, 255);
    private static final Color COLOR_CAJERO_CERRADO = new Color(255, 100, 100);
    private static final Color COLOR_CLIENTE        = new Color(255, 220, 160);
    private static final Color COLOR_FONDO          = new Color(245, 248, 255);
    private static final Color COLOR_CARRIL         = new Color(230, 238, 255);
    private static final Color COLOR_RAPIDO         = new Color(140, 255, 180);
    private static final Color COLOR_TEXTO_CAJERO   = new Color(30, 60, 120);
    private static final Color COLOR_TEXTO_CLIENTE  = new Color(100, 60, 10);
    private static final Color COLOR_HEADER         = new Color(100, 140, 220);

    private static final int CUADRO_ANCHO = 72;
    private static final int CUADRO_ALTO  = 46;
    private static final int PADDING      = 14;
    private static final int CARRIL_ALTO  = 80;
    private static final int HEADER_ALTO  = 70;
    private static final int CAJERO_CUADRO = 90;

    private SimulacionBanco simulacion;
    private PanelSimulacion panel;
    private javax.swing.Timer animTimer;
    private SonidoManager sonido;
    private JLabel lblCancion;
    private JButton btnPausa;
    private JSlider sliderVolumen;

    // Posición actual animada por cliente: id -> {x, y}
    // Cuando un cliente aparece por primera vez, su x inicial está desplazada a la derecha
    private ConcurrentHashMap<Integer, double[]> posiciones = new ConcurrentHashMap<>();

    public InterfazGrafica(SimulacionBanco simulacion, SonidoManager sonido) {
        this.simulacion = simulacion;
        this.sonido = sonido;
        configurarVentana();
        crearPanel();
        crearReproductor();
        iniciarAnimaciones();
        sonido.setOnCancionCambia(() -> SwingUtilities.invokeLater(this::actualizarReproductor));
    }

    private void configurarVentana() {
        setTitle("Simulación Banco — Cajeros");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setBackground(COLOR_FONDO);
        setSize(1100, HEADER_ALTO + CARRIL_ALTO * 4 + 60);
        setLocationRelativeTo(null);
    }

    private void crearReproductor() {
        // Panel flotante esquina inferior derecha
        JPanel reproductor = new JPanel();
        reproductor.setLayout(new BoxLayout(reproductor, BoxLayout.Y_AXIS));
        reproductor.setBackground(new Color(30, 40, 70, 220));
        reproductor.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 140, 220), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        // Etiqueta "♪ Now Playing"
        JLabel lblTitulo = new JLabel(">> Now Playing");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblTitulo.setForeground(new Color(140, 180, 255));
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Nombre de la canción
        lblCancion = new JLabel("...");
        lblCancion.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCancion.setForeground(Color.WHITE);
        lblCancion.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        panelBotones.setOpaque(false);

        btnPausa = new JButton("|| Pausa");
        btnPausa.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnPausa.setForeground(Color.WHITE);
        btnPausa.setBackground(new Color(60, 80, 140));
        btnPausa.setBorderPainted(false);
        btnPausa.setFocusPainted(false);
        btnPausa.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPausa.addActionListener(e -> {
            sonido.togglePausa();
            actualizarReproductor();
        });

        JButton btnSkip = new JButton(">> Skip");
        btnSkip.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnSkip.setForeground(Color.WHITE);
        btnSkip.setBackground(new Color(60, 80, 140));
        btnSkip.setBorderPainted(false);
        btnSkip.setFocusPainted(false);
        btnSkip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSkip.addActionListener(e -> sonido.siguienteCancion());

        panelBotones.add(btnPausa);
        panelBotones.add(btnSkip);

        // Slider de volumen
        sliderVolumen = new JSlider(0, 100, (int)(sonido.getVolumen() * 100));
        sliderVolumen.setOpaque(false);
        sliderVolumen.setForeground(new Color(140, 180, 255));
        sliderVolumen.setMaximumSize(new Dimension(160, 20));
        sliderVolumen.setPreferredSize(new Dimension(140, 20));
        sliderVolumen.addChangeListener(e ->
            sonido.setVolumen(sliderVolumen.getValue() / 100f)
        );

        JLabel lblVol = new JLabel("Vol.");
        lblVol.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblVol.setForeground(new Color(180, 200, 240));
        lblVol.setAlignmentX(Component.CENTER_ALIGNMENT);

        reproductor.add(lblTitulo);
        reproductor.add(Box.createVerticalStrut(3));
        reproductor.add(lblCancion);
        reproductor.add(Box.createVerticalStrut(5));
        reproductor.add(panelBotones);
        reproductor.add(Box.createVerticalStrut(4));
        reproductor.add(lblVol);
        reproductor.add(sliderVolumen);

        // Posicionar en esquina inferior derecha usando LayeredPane
        getLayeredPane().add(reproductor, JLayeredPane.PALETTE_LAYER);

        // Reposicionar cuando cambie el tamaño
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                Dimension size = reproductor.getPreferredSize();
                reproductor.setBounds(
                    getWidth() - size.width - 20,
                    getHeight() - size.height - 40,
                    size.width, size.height
                );
            }
        });

        // Posición inicial
        reproductor.setBounds(getWidth() - 200, getHeight() - 160, 180, 140);

        // Forzar texto inicial por si la canción ya empezó
        actualizarReproductor();
    }

    private void actualizarReproductor() {
        if (lblCancion == null) return;
        lblCancion.setText(sonido.getNombreCancionActual());
        btnPausa.setText(sonido.isPausado() ? "> Play" : "|| Pausa");
    }

    private void crearPanel() {
        panel = new PanelSimulacion();
        panel.setBackground(COLOR_FONDO);
        add(panel);
    }

    private void iniciarAnimaciones() {
        animTimer = new javax.swing.Timer(30, e -> {
            actualizarPosiciones();
            panel.repaint();
        });
        animTimer.start();
    }

    private void actualizarPosiciones() {
        List<Cajero> cajeros = simulacion.getCajeros();

        for (int i = 0; i < cajeros.size(); i++) {
            Cajero cajero = cajeros.get(i);
            int carritoY = HEADER_ALTO + i * CARRIL_ALTO;
            int startX   = PADDING + CAJERO_CUADRO + PADDING * 2;
            int targetY  = carritoY + (CARRIL_ALTO - CUADRO_ALTO) / 2;

            // Cliente en atención: posición 0, junto al cajero
            Cliente enAtencion = cajero.getClienteEnAtencion();
            int offsetCola = 0;
            if (enAtencion != null) {
                int targetX = startX;
                double[] pos = posiciones.computeIfAbsent(
                    enAtencion.getId(),
                    k -> new double[]{ targetX + 250, targetY }
                );
                pos[0] += (targetX - pos[0]) * 0.18;
                pos[1] += (targetY - pos[1]) * 0.18;
                offsetCola = 1;
            }

            List<Cliente> cola = new ArrayList<>(cajero.getCola());
            for (int j = 0; j < cola.size(); j++) {
                Cliente c = cola.get(j);
                int targetX = startX + (j + offsetCola) * (CUADRO_ANCHO + PADDING);

                double[] pos = posiciones.computeIfAbsent(
                    c.getId(),
                    k -> new double[]{ targetX + 250, targetY }
                );

                pos[0] += (targetX - pos[0]) * 0.18;
                pos[1] += (targetY - pos[1]) * 0.18;
            }
        }

        // Limpiar clientes que ya no están en cola NI siendo atendidos
        Set<Integer> idsActivos = new HashSet<>();
        for (Cajero cajero : cajeros) {
            for (Cliente c : cajero.getCola()) {
                idsActivos.add(c.getId());
            }
            Cliente enAtencion = cajero.getClienteEnAtencion();
            if (enAtencion != null) idsActivos.add(enAtencion.getId());
        }
        posiciones.keySet().retainAll(idsActivos);
    }

    public void mostrarFin() {
        SwingUtilities.invokeLater(() -> {
            animTimer.stop();
            panel.repaint();

            JDialog dialogo = new JDialog(this, "¡Simulación Finalizada!", true);
            dialogo.setSize(380, 200);
            dialogo.setLocationRelativeTo(this);

            JPanel contenido = new JPanel(new BorderLayout());
            contenido.setBackground(COLOR_FONDO);

            JLabel titulo = new JLabel("🎉 ¡50 Clientes Atendidos!", SwingConstants.CENTER);
            titulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
            titulo.setForeground(COLOR_TEXTO_CAJERO);
            titulo.setBorder(BorderFactory.createEmptyBorder(30, 20, 10, 20));

            long tiempoTotal = (System.currentTimeMillis() - simulacion.getTiempoInicio()) / 1000;
            JLabel sub = new JLabel("Tiempo total: " + formatTiempo(tiempoTotal), SwingConstants.CENTER);
            sub.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            sub.setForeground(new Color(80, 80, 120));

            JButton cerrar = new JButton("Cerrar");
            cerrar.setBackground(COLOR_HEADER);
            cerrar.setForeground(Color.WHITE);
            cerrar.setFont(new Font("Segoe UI", Font.BOLD, 14));
            cerrar.setBorderPainted(false);
            cerrar.setFocusPainted(false);
            cerrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            cerrar.addActionListener(ev -> System.exit(0));

            JPanel botones = new JPanel(new FlowLayout());
            botones.setBackground(COLOR_FONDO);
            botones.add(cerrar);
            botones.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));

            contenido.add(titulo, BorderLayout.NORTH);
            contenido.add(sub, BorderLayout.CENTER);
            contenido.add(botones, BorderLayout.SOUTH);
            dialogo.add(contenido);
            dialogo.setVisible(true);
        });
    }

    private String formatTiempo(long segundos) {
        return String.format("%02d:%02d", segundos / 60, segundos % 60);
    }

    // ==================== PANEL ====================
    class PanelSimulacion extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            dibujarFondo(g2);
            dibujarHeader(g2);
            dibujarCajeros(g2);
            if (simulacion.isTerminado()) dibujarOverlayFin(g2);
        }

        private void dibujarFondo(Graphics2D g2) {
            g2.setColor(COLOR_FONDO);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        private void dibujarHeader(Graphics2D g2) {
            GradientPaint grad = new GradientPaint(0, 0, COLOR_HEADER, getWidth(), HEADER_ALTO, new Color(80, 120, 200));
            g2.setPaint(grad);
            g2.fillRect(0, 0, getWidth(), HEADER_ALTO);

            // Contador
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
            String atendidos = "Clientes Atendidos: " + simulacion.getClientesAtendidos() + " / " + simulacion.getMetaClientes();
            g2.drawString(atendidos, 30, 30);

            // Barra de progreso
            int barraW = 300, barraX = 30, barraY = 42;
            g2.setColor(new Color(255, 255, 255, 60));
            g2.fillRoundRect(barraX, barraY, barraW, 14, 10, 10);
            int progreso = (int)((simulacion.getClientesAtendidos() / (double) simulacion.getMetaClientes()) * barraW);
            g2.setColor(new Color(140, 255, 180));
            g2.fillRoundRect(barraX, barraY, progreso, 14, 10, 10);

            // Cronómetro
            long elapsed = (System.currentTimeMillis() - simulacion.getTiempoInicio()) / 1000;
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            String tiempo = "Tiempo: " + formatTiempo(elapsed);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(tiempo, getWidth() - fm.stringWidth(tiempo) - 30, 30);

            // Leyenda
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.setColor(new Color(255, 255, 255, 200));
            g2.drawString("[ ] Abierto", getWidth() - 230, 55);
            g2.setColor(new Color(255, 120, 120, 220));
            g2.drawString("[ ] Cerrado", getWidth() - 155, 55);
            g2.setColor(new Color(140, 255, 180, 220));
            g2.drawString("[*] Rapido", getWidth() - 75, 55);
        }

        private void dibujarCajeros(Graphics2D g2) {
            List<Cajero> cajeros = simulacion.getCajeros();
            for (int i = 0; i < cajeros.size(); i++) {
                int y = HEADER_ALTO + i * CARRIL_ALTO;
                dibujarCarril(g2, cajeros.get(i), y, i);
            }
        }

        private void dibujarCarril(Graphics2D g2, Cajero cajero, int y, int idx) {
            // Fondo alternado
            g2.setColor(idx % 2 == 0 ? COLOR_CARRIL : new Color(238, 244, 255));
            g2.fillRect(0, y, getWidth(), CARRIL_ALTO);
            g2.setColor(new Color(200, 215, 240));
            g2.drawLine(0, y + CARRIL_ALTO - 1, getWidth(), y + CARRIL_ALTO - 1);

            dibujarCuadroCajero(g2, cajero, PADDING, y + (CARRIL_ALTO - (CAJERO_CUADRO - 10)) / 2);
            dibujarClientesCola(g2, cajero);
        }

        private void dibujarCuadroCajero(Graphics2D g2, Cajero cajero, int x, int y) {
            int h = CAJERO_CUADRO - 10;
            Color color = cajero.isAbierto()
                    ? (cajero.isRapido() ? COLOR_RAPIDO : COLOR_CAJERO_ABIERTO)
                    : COLOR_CAJERO_CERRADO;

            // Sombra
            g2.setColor(new Color(0, 0, 0, 30));
            g2.fillRoundRect(x + 3, y + 3, CAJERO_CUADRO, h, 14, 14);

            // Cuerpo
            g2.setColor(color);
            g2.fillRoundRect(x, y, CAJERO_CUADRO, h, 14, 14);

            // Borde
            g2.setColor(cajero.isAbierto() ? new Color(100, 160, 220) : new Color(180, 40, 40));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x, y, CAJERO_CUADRO, h, 14, 14);
            g2.setStroke(new BasicStroke(1f));

            // Nombre
            g2.setColor(cajero.isAbierto() ? COLOR_TEXTO_CAJERO : Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            String nombre = cajero.getNombre();
            g2.drawString(nombre, x + (CAJERO_CUADRO - fm.stringWidth(nombre)) / 2, y + h / 2 + 3);

            // Estado
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            String estado = cajero.isAbierto() ? (cajero.isRapido() ? "RAPIDO" : "ABIERTO") : "CERRADO";
            g2.setColor(cajero.isAbierto() ? new Color(60, 100, 180, 180) : new Color(255, 255, 255, 200));
            fm = g2.getFontMetrics();
            g2.drawString(estado, x + (CAJERO_CUADRO - fm.stringWidth(estado)) / 2, y + h / 2 + 17);
        }

        private void dibujarClientesCola(Graphics2D g2, Cajero cajero) {
            // Dibujar cliente en atención (resaltado levemente)
            Cliente enAtencion = cajero.getClienteEnAtencion();
            if (enAtencion != null) {
                double[] pos = posiciones.get(enAtencion.getId());
                if (pos != null) {
                    dibujarCuadroCliente(g2, enAtencion.getNombre(), (int) pos[0], (int) pos[1], true);
                }
            }
            // Dibujar cola normal
            List<Cliente> cola = new ArrayList<>(cajero.getCola());
            for (Cliente cliente : cola) {
                double[] pos = posiciones.get(cliente.getId());
                if (pos == null) continue;
                dibujarCuadroCliente(g2, cliente.getNombre(), (int) pos[0], (int) pos[1], false);
            }
        }

        private void dibujarCuadroCliente(Graphics2D g2, String nombre, int x, int y, boolean enAtencion) {
            // Sombra
            g2.setColor(new Color(0, 0, 0, 25));
            g2.fillRoundRect(x + 2, y + 2, CUADRO_ANCHO, CUADRO_ALTO, 10, 10);

            // Cuerpo (verde claro si está siendo atendido)
            g2.setColor(enAtencion ? new Color(180, 255, 200) : COLOR_CLIENTE);
            g2.fillRoundRect(x, y, CUADRO_ANCHO, CUADRO_ALTO, 10, 10);

            // Borde (verde oscuro si está siendo atendido)
            g2.setColor(enAtencion ? new Color(60, 160, 90) : new Color(200, 150, 80));
            g2.setStroke(new BasicStroke(enAtencion ? 2.5f : 1.5f));
            g2.drawRoundRect(x, y, CUADRO_ANCHO, CUADRO_ALTO, 10, 10);
            g2.setStroke(new BasicStroke(1f));

            // Nombre (truncado si es largo)
            g2.setColor(COLOR_TEXTO_CLIENTE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            String txt = nombre;
            if (fm.stringWidth(txt) > CUADRO_ANCHO - 8) {
                while (nombre.length() > 3 && fm.stringWidth(nombre + "…") > CUADRO_ANCHO - 8)
                    nombre = nombre.substring(0, nombre.length() - 1);
                txt = nombre + "…";
            }
            g2.drawString(txt, x + (CUADRO_ANCHO - fm.stringWidth(txt)) / 2,
                    y + CUADRO_ALTO / 2 + fm.getAscent() / 2 - 2);
        }

        private void dibujarOverlayFin(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 32));
            String msg = "¡Simulación Completada!";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
        }
    }
}
