import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SonidoManager {

    // ── Archivos de efectos ──────────────────────────────────────────────────
    private static final String ARCHIVO_ATENDIDO = "cliente_atendido.wav";
    private static final String ARCHIVO_FIN      = "simulacion_fin.wav";

    // ── Playlist: agrega aquí todos tus .wav de música ──────────────────────
    // Coloca los archivos en la raíz del proyecto (junto a src/)
    private static final String[] CANCIONES = {
        "Sunburst.wav",
        "Hellcat.wav",
        "Hello.wav"
    };

    // ── Estado del reproductor ───────────────────────────────────────────────
    private List<String>  playlist       = new ArrayList<>();
    private int           indiceActual   = 0;
    private Clip          clipMusica     = null;
    private boolean       pausado        = false;
    private float         volumen        = 0.8f;
    private Runnable      onCancionCambia;

    // ════════════════════════════════════════════════════════════════════════
    //  MÚSICA DE FONDO
    // ════════════════════════════════════════════════════════════════════════

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
            if (!f.exists()) {
                System.out.println("[SONIDO] No se encontró: " + archivo);
                siguienteCancion();
                return;
            }
            AudioInputStream stream = AudioSystem.getAudioInputStream(f);
            clipMusica = AudioSystem.getClip();
            clipMusica.open(stream);
            aplicarVolumen();
            clipMusica.start();

            // Guardar referencia al clip actual para el listener
            final Clip clipActual = clipMusica;
            clipActual.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP && !pausado && clipActual == clipMusica) {
                    siguienteCancion();
                }
            });

            System.out.println("[SONIDO] ♪ Reproduciendo: " + getNombreCancionActual());
            if (onCancionCambia != null) onCancionCambia.run();

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.out.println("[SONIDO] Error con " + archivo + ": " + e.getMessage());
            siguienteCancion();
        }
    }

    public void siguienteCancion() {
        pausado = false;
        indiceActual = (indiceActual + 1) % playlist.size();
        reproducirCancionActual();
    }

    public void togglePausa() {
        if (clipMusica == null) return;
        if (pausado) {
            pausado = false;
            clipMusica.start();
        } else {
            pausado = true;  // primero marcar, luego detener
            clipMusica.stop();
        }
        if (onCancionCambia != null) onCancionCambia.run();
    }

    public void setVolumen(float v) {
        this.volumen = Math.max(0f, Math.min(1f, v));
        aplicarVolumen();
    }

    private void aplicarVolumen() {
        if (clipMusica == null) return;
        try {
            FloatControl ctrl = (FloatControl) clipMusica.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (volumen == 0f) ? ctrl.getMinimum()
                                       : 20f * (float) Math.log10(volumen);
            ctrl.setValue(Math.max(ctrl.getMinimum(), Math.min(ctrl.getMaximum(), dB)));
        } catch (IllegalArgumentException ignored) {}
    }

    private void detenerClipActual() {
        if (clipMusica != null) {
            clipMusica.stop();
            clipMusica.close();
            clipMusica = null;
        }
    }

    public void detenerMusicaFondo() {
        pausado = false;
        detenerClipActual();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  EFECTOS DE SONIDO
    // ════════════════════════════════════════════════════════════════════════

    private void reproducirEfecto(String archivoWav) {
        new Thread(() -> {
            try {
                File f = new File(archivoWav);
                if (!f.exists()) { System.out.println("[SONIDO] No se encontró: " + archivoWav); return; }
                AudioInputStream stream = AudioSystem.getAudioInputStream(f);
                Clip clip = AudioSystem.getClip();
                clip.open(stream);
                clip.start();
                clip.addLineListener(e -> { if (e.getType() == LineEvent.Type.STOP) clip.close(); });
            } catch (Exception e) {
                System.out.println("[SONIDO] Error efecto " + archivoWav + ": " + e.getMessage());
            }
        }, "HiloEfecto").start();
    }

    public void sonarClienteAtendido() { reproducirEfecto(ARCHIVO_ATENDIDO); }

    public void sonarFin() {
        detenerMusicaFondo();
        reproducirEfecto(ARCHIVO_FIN);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GETTERS PARA LA UI
    // ════════════════════════════════════════════════════════════════════════

    public String getNombreCancionActual() {
        if (playlist.isEmpty()) return "Sin música";
        String nombre = playlist.get(indiceActual);
        nombre = nombre.replaceAll(".*[\\\\/]", "").replaceAll("\\.wav$", "");
        return nombre;
    }

    public boolean isPausado() { return pausado; }
    public float   getVolumen() { return volumen; }

    public void setOnCancionCambia(Runnable callback) { this.onCancionCambia = callback; }
}
