import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;

public class Puja {
    public String nombre;
    public int precio;
    public AID ganador;
    public ArrayList<AID> participantes;

    public Puja() {
        nombre = null;
        precio = 5;
        ganador = null;
        participantes = new ArrayList<AID>();
    }

    public Puja(String nombre, int precio) {
        this.nombre = nombre;
        this.precio = precio;
        ganador = null;
        participantes = new ArrayList<AID>();
    }
}
