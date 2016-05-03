import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.HashMap;

public class Cliente extends Agent {

    public AID vendedor;
    public HashMap<String, Integer> libros;
    public HashMap<String, Integer> pujas;

    public void setup() {
        libros = new HashMap<String, Integer>();
        pujas = new HashMap<String, Integer>();
        libros.put("pfd", 20);
        
        System.out.println("Libros añadidos");

        addBehaviour(new Buscador());
        System.out.println("Añadido primer comportamiento");
        
        addBehaviour(new TickerBehaviour(this, 10000) {
            protected void onTick() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                boolean bucle = true;
                while (bucle) {
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
                        String libro = msg.getContent();
                        pujas.put(libro, libros.get(libro));
                        libros.remove(libro);
                        System.out.println("El siguiente libro está disponible: "+libro);
                    } else {
                        System.out.println("Búsqueda de libros finalizada");
                        bucle = false;
                    }
                }
                for (String key : libros.keySet()) {
                    ACLMessage busqueda = new ACLMessage(jade.lang.acl.ACLMessage.REQUEST);
                    busqueda.addReceiver(vendedor);
                    busqueda.setContent(key);
                    myAgent.send(busqueda);
                    System.out.println("Mensaje enviado para el libro: "+key);
                }
                System.out.println("Mensajes de petición pedidos");
            }
        });
        System.out.println("Vamos a hacer el doDelete");
    }
    
    public void takeDown() {
        System.out.println("AdiosCliente");
    }

    public class Buscador extends OneShotBehaviour {

        public void action() {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("vendedor");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                vendedor = result[0].getName();
                System.out.println("Try realizado");
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
            System.out.println("Vendedor encontrado");
        }
    }
}
