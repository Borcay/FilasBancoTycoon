import java.util.Random;

public class Cliente {

    private static final String[] NOMBRES = {
        "Andrés", "Sofía", "Carlos", "María", "Luis", "Ana", "Pedro", "Laura",
        "Miguel", "Paula", "Diego", "Valeria", "Jorge", "Camila", "Sergio",
        "Natalia", "Ricardo", "Isabella", "Fernando", "Daniela", "Tomás",
        "Lucía", "Mateo", "Elena", "Nicolás", "Martina", "Alejandro", "Sara",
        "Julián", "Gabriela", "Emilio", "Verónica", "Adrián", "Patricia",
        "Roberto", "Claudia", "Héctor", "Mónica", "Esteban", "Rebeca",
        "Santiago", "Valentina", "Sebastián", "Mariana", "Camilo", "Catalina",
        "Rodrigo", "Melissa", "Felipe", "Andrea", "Iván", "Carolina", "Gustavo",
        "Ximena", "Eduardo", "Paola", "Mauricio", "Liliana", "Oscar", "Diana",
        "Jhon", "Leidy", "Wilson", "Yenny", "Cristian", "Nathalia", "Brayan",
        "Tatiana", "Steven", "Lorena", "Kevin", "Manuela", "Andrés Felipe",
        "María José", "Juan Pablo", "Laura Sofía", "Sebastián A.", "Ana María",
        "Darío", "Esperanza", "Fabián", "Gloria", "Hernán", "Irene",
        "Jaime", "Karen", "Leonardo", "Miriam", "Néstor", "Olga",
        "Pilar", "Quintero", "Raúl", "Susana", "Uriel", "Viviana",
        "Walter", "Xiomara", "Yolanda", "Zaira", "Álvaro", "Beatriz",
        "César", "Dolores", "Ernesto", "Flor", "Gilberto", "Hilda"
    };

    private static final Random random = new Random();
    private static int contadorId = 1;

    private final int id;
    private final String nombre;
    private final boolean vip;       // cliente VIP da recompensa doble
    private boolean atendido;

    // ── posición animada (lerp hacia targetX/Y) ──
    private double x, y;
    private double targetX, targetY;
    private float alpha = 1.0f;
    private boolean desapareciendo = false;

    public Cliente(boolean vip) {
        this.id     = contadorId++;
        this.nombre = NOMBRES[random.nextInt(NOMBRES.length)];
        this.vip    = vip;
        this.atendido = false;
    }

    // ── getters / setters ──
    public int     getId()      { return id; }
    public String  getNombre()  { return nombre; }
    public boolean isVip()      { return vip; }
    public boolean isAtendido() { return atendido; }
    public void    setAtendido(boolean v) { this.atendido = v; }

    public double getX()  { return x; }  public void setX(double v) { x = v; }
    public double getY()  { return y; }  public void setY(double v) { y = v; }
    public double getTargetX() { return targetX; } public void setTargetX(double v) { targetX = v; }
    public double getTargetY() { return targetY; } public void setTargetY(double v) { targetY = v; }
    public float  getAlpha()   { return alpha; }   public void setAlpha(float v)    { alpha = v; }
    public boolean isDesapareciendo() { return desapareciendo; }
    public void   setDesapareciendo(boolean v) { desapareciendo = v; }

    @Override
    public String toString() { return nombre + (vip ? " ★" : ""); }
}
