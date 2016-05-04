
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
    public HashMap<String, Integer> noDisponibles;

    public void setup() {
        libros = new HashMap<String, Integer>();
        pujas = new HashMap<String, Integer>();
        noDisponibles = new HashMap<String, Integer>();
        libros.put("pfd", 20);
        noDisponibles.put("pfd", 20);

        addBehaviour(new Buscador());
        System.out.println("Añadido primer comportamiento");

        addBehaviour(new TickerBehaviour(this, 30000) {
            protected void onTick() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL);
                boolean bucle = true;
                while(bucle){
                    ACLMessage msg = myAgent.receive(mt);
                    if(msg!=null){
                        pujas.remove(msg.getContent());
                        System.out.println("\t\t\tNo he ganado el libro: "+msg.getContent());
                    }else{
                        bucle=false;
                    }
                }
                bucle=true;
                
                mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                while(bucle){
                    ACLMessage msg = myAgent.receive(mt);
                    if(msg != null){
                        pujas.remove(msg.getContent());
                        System.out.println("\t\t\tHe ganado el libro: "+msg.getContent());
                    }else{
                        bucle=false;
                    }
                }
                bucle=true;
                
                mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                while (bucle) {
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
                        String contenido[] = msg.getContent().split(";");
                        if (pujas.containsKey(contenido[0])) {
                            pujas.replace(contenido[0], Integer.parseInt(contenido[1]));
                        } else {
                            pujas.put(contenido[0], Integer.parseInt(contenido[1]));
                            noDisponibles.remove(contenido[0]);
                        }
                        System.out.println("\t\t\tEl siguiente libro está disponible: " + contenido[0]);
                    } else {
                        bucle = false;
                    }
                }

                for (String key : pujas.keySet()) {
                    if (pujas.get(key) < libros.get(key)) {
                        ACLMessage pujando = new ACLMessage(ACLMessage.PROPOSE);
                        pujando.addReceiver(vendedor);
                        pujando.setContent(key + ";" + pujas.get(key));
                        myAgent.send(pujando);
                    }
                }

                for (String key : noDisponibles.keySet()) {
                    ACLMessage busqueda = new ACLMessage(jade.lang.acl.ACLMessage.REQUEST);
                    busqueda.addReceiver(vendedor);
                    busqueda.setContent(key);
                    myAgent.send(busqueda);
                    System.out.println("\t\t\tMensaje enviado para el libro: " + key);
                }
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
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
            System.out.println("Vendedor encontrado");
        }
    }
}
