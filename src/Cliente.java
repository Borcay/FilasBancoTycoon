import java.util.Random;

public class Cliente {
    private static final String[] NOMBRES = {
        "Andrés", "Sofía", "Carlos", "María", "Luis", "Ana", "Pedro", "Laura",
        "Miguel", "Paula", "Diego", "Valeria", "Jorge", "Camila", "Sergio",
        "Natalia", "Ricardo", "Isabella", "Fernando", "Daniela", "Tomás",
        "Lucía", "Mateo", "Elena", "Nicolás", "Martina", "Alejandro", "Sara",
        "Julián", "Gabriela", "Emilio", "Verónica", "Adrián", "Patricia",
        "Roberto", "Claudia", "Héctor", "Mónica", "Esteban", "Rebeca"
    };

    private static final Random random = new Random();
    private static int contadorId = 1;

    private int id;
    private String nombre;
    private boolean atendido;

    // Estado de animación (para la GUI)
    private double x;
    private double y;
    private double targetX;
    private double targetY;
    private boolean animando;
    private boolean desapareciendo;
    private float alpha; // para fade out

    public Cliente() {
        this.id = contadorId++;
        this.nombre = NOMBRES[random.nextInt(NOMBRES.length)];
        this.atendido = false;
        this.alpha = 1.0f;
        this.animando = false;
        this.desapareciendo = false;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public boolean isAtendido() { return atendido; }
    public void setAtendido(boolean atendido) { this.atendido = atendido; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public double getTargetX() { return targetX; }
    public void setTargetX(double targetX) { this.targetX = targetX; }
    public double getTargetY() { return targetY; }
    public void setTargetY(double targetY) { this.targetY = targetY; }
    public boolean isAnimando() { return animando; }
    public void setAnimando(boolean animando) { this.animando = animando; }
    public boolean isDesapareciendo() { return desapareciendo; }
    public void setDesapareciendo(boolean desapareciendo) { this.desapareciendo = desapareciendo; }
    public float getAlpha() { return alpha; }
    public void setAlpha(float alpha) { this.alpha = alpha; }

    @Override
    public String toString() {
        return nombre;
    }
}
