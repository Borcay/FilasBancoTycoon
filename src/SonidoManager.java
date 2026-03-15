import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class SonidoManager {

    // ── Singleton global ─────────────────────────────────────────────────
    private static SonidoManager instancia;
    public static SonidoManager get() {
        if (instancia == null) instancia = new SonidoManager();
        return instancia;
    }
    private SonidoManager() {}  // constructor privado

    private static final String ARCHIVO_ATENDIDO     = "cliente_atendido.wav";
    private static final String ARCHIVO_FIN          = "simulacion_fin.wav";
    private static final String ARCHIVO_LADRON_LLEGA = "ladron_aparece.wav";
    private static final String ARCHIVO_LADRON_ROBA  = "ladron_roba.wav";

    private static final String[] CANCIONES = {
        "Sunburst.wav",
        "Hellcat.wav",
        "Hello.wav"
    };

    private final List<String> playlist   = new ArrayList<>();
    private int    indiceActual            = 0;
    private Clip   clipMusica              = null;
    private boolean pausado               = false;
    private float  volumen                = 0.8f;
    private Runnable onCancionCambia;

    // ── Música de fondo ──
    public void iniciarMusicaFondo() {
        for (String c : CANCIONES) playlist.add(c);
        Collections.shuffle(playlist);
        indiceActual = 0;
        reproducirCancionActual();
    }

    private void reproducirCancionActual() {
        if (playlist.isEmpty()) return;
        detenerClipActual();

        String archivo = playlist.get(indiceActual);
        try {
            File f = new File(archivo);
            if (!f.exists()) { System.out.println("[SONIDO] No encontrado: " + archivo); siguienteCancion(); return; }
            AudioInputStream stream = AudioSystem.getAudioInputStream(f);
            clipMusica = AudioSystem.getClip();
            clipMusica.open(stream);
            aplicarVolumen();
            clipMusica.start();

            final Clip clipActual = clipMusica;
            clipActual.addLineListener(ev -> {
                if (ev.getType() == LineEvent.Type.STOP && !pausado && clipActual == clipMusica)
                    siguienteCancion();
            });

            if (onCancionCambia != null) onCancionCambia.run();

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.out.println("[SONIDO] Error: " + e.getMessage());
            siguienteCancion();
        }
    }

    public void siguienteCancion() {
        pausado = false;
        indiceActual = (indiceActual + 1) % Math.max(1, playlist.size());
        reproducirCancionActual();
    }

    public void togglePausa() {
        if (clipMusica == null) return;
        if (pausado) { pausado = false; clipMusica.start(); }
        else         { pausado = true;  clipMusica.stop();  }
        if (onCancionCambia != null) onCancionCambia.run();
    }

    public void setVolumen(float v) {
        volumen = Math.max(0f, Math.min(1f, v));
        aplicarVolumen();
    }

    private void aplicarVolumen() {
        if (clipMusica == null) return;
        try {
            FloatControl ctrl = (FloatControl) clipMusica.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (volumen <= 0f) ? ctrl.getMinimum() : 20f * (float) Math.log10(volumen);
            ctrl.setValue(Math.max(ctrl.getMinimum(), Math.min(ctrl.getMaximum(), dB)));
        } catch (IllegalArgumentException ignored) {}
    }

    private void detenerClipActual() {
        if (clipMusica != null) { clipMusica.stop(); clipMusica.close(); clipMusica = null; }
    }

    public void detenerMusicaFondo() { pausado = false; detenerClipActual(); }

    // ── Efectos ──
    private void reproducirEfecto(String archivo) {
        new Thread(() -> {
            try {
                File f = new File(archivo);
                if (!f.exists()) return;
                AudioInputStream stream = AudioSystem.getAudioInputStream(f);
                Clip clip = AudioSystem.getClip();
                clip.open(stream);
                clip.start();
                clip.addLineListener(ev -> { if (ev.getType() == LineEvent.Type.STOP) clip.close(); });
            } catch (Exception e) { System.out.println("[SONIDO] Efecto error: " + e.getMessage()); }
        }).start();
    }

    public void sonarClienteAtendido() { reproducirEfecto(ARCHIVO_ATENDIDO); }
    public void sonarFin()             { detenerMusicaFondo(); reproducirEfecto(ARCHIVO_FIN); }
    public void sonarLadronAparece()   { reproducirEfecto(ARCHIVO_LADRON_LLEGA); }
    public void sonarLadronRoba()      { reproducirEfecto(ARCHIVO_LADRON_ROBA); }

    // ── Getters para la UI ──
    public String getNombreCancionActual() {
        if (playlist.isEmpty()) return "Sin música";
        return playlist.get(indiceActual).replaceAll(".*[\\\\/]","").replaceAll("\\.wav$","");
    }
    public boolean isPausado() { return pausado; }
    public float   getVolumen(){ return volumen; }
    public boolean isReproduciendo() { return clipMusica != null && clipMusica.isRunning(); }
    public void    setOnCancionCambia(Runnable r) { onCancionCambia = r; }
}
