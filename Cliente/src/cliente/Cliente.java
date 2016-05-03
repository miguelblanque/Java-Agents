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
        libros.put("pfd", 20);

        addBehaviour(new Buscador());
        addBehaviour(new TickerBehaviour(this, 30000) {
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
                        bucle = false;
                    }
                }
                for (String key : libros.keySet()) {
                    ACLMessage busqueda = new ACLMessage(jade.lang.acl.ACLMessage.INFORM);
                    busqueda.addReceiver(vendedor);
                    busqueda.setContent(key);
                }

            }
        });
        doDelete();
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
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
    }
}
