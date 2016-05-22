import jade.core.AID;
import java.util.ArrayList;

public class DataContainer {
    public String nombre;
    public AID ganador;
    public int precio;
    public int incremento;
    public ArrayList<AID> participantes;
    
    public DataContainer(String nombre, int precio, int incremento){
        this.nombre=nombre;
        this.ganador=null;
        this.precio=precio;
        this.incremento=incremento;
        this.participantes=new ArrayList<AID>();
    }
}
