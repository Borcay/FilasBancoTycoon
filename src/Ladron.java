import java.util.Random;

/**
 * Ladron: cliente especial que roba monedas si no es eliminado a tiempo.
 * El jugador debe hacer clic sobre él para que el guardia lo saque.
 */
public class Ladron extends Cliente {

    private static final String[] APODOS = {
        "El Rata", "El Sombra", "El Caco", "El Tuso", "El Pillo",
        "El Bicho", "El Zorrito", "El Gancho", "El Mapache", "El Liso"
    };

    private static final Random rnd = new Random();

    private volatile boolean atrapado = false;
    private volatile boolean robando  = false;
    private volatile boolean eliminado = false;

    // animación policia
    private volatile boolean mostrandoPolicia = false;
    private volatile float   alphaPolicia     = 0f;

    public Ladron() {
        super(false); // no es VIP
    }

    @Override
    public String getNombre() {
        return APODOS[getId() % APODOS.length];
    }

    public boolean isAtrapado()          { return atrapado; }
    public void    setAtrapado(boolean v){ atrapado = v; }

    public boolean isRobando()           { return robando; }
    public void    setRobando(boolean v) { robando = v; }

    public boolean isEliminado()         { return eliminado; }
    public void    setEliminado(boolean v){ eliminado = v; }

    public boolean isMostrandoPolicia()  { return mostrandoPolicia; }
    public void    setMostrandoPolicia(boolean v){ mostrandoPolicia = v; }

    public float  getAlphaPolicia()      { return alphaPolicia; }
    public void   setAlphaPolicia(float v){ alphaPolicia = v; }
}
