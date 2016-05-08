
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
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
    public HashMap<String, Integer> salida;

    public void setup() {
        Principal gui = new Principal(this);
        gui.setVisible(true);
        gui.setTitle("Cliente");

        libros = new HashMap<String, Integer>();
        pujas = new HashMap<String, Integer>();
        noDisponibles = new HashMap<String, Integer>();
        salida = new HashMap<String, Integer>();

        addBehaviour(new Buscador());
        System.out.println("Añadido primer comportamiento");

        addBehaviour(new TickerBehaviour(this, 10000) {
            protected void onTick() {
                for (String key : salida.keySet()) {
                    ACLMessage msg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                    msg.setSender(vendedor);
                    msg.setContent(key);
                    myAgent.send(msg);
                    salida.remove(key);
                }

                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL);
                boolean bucle = true;
                while (bucle) {
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
                        pujas.remove(msg.getContent());
                        gui.jTextArea1.setText(gui.jTextArea1.getText().replace(msg.getContent() + "-Pujando", msg.getContent() + "-Puja Perdida"));
                        System.out.println("\t\t\tNo he ganado el libro: " + msg.getContent());
                    } else {
                        bucle = false;
                    }
                }
                bucle = true;

                mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                while (bucle) {
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
                        pujas.remove(msg.getContent());
                        gui.jTextArea1.setText(gui.jTextArea1.getText().replace(msg.getContent() + "-Pujando", msg.getContent() + "-Ganado"));
                        System.out.println("\t\t\tHe ganado el libro: " + msg.getContent());
                    } else {
                        bucle = false;
                    }
                }
                bucle = true;

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
                            gui.jTextArea1.setText(gui.jTextArea1.getText().replace(contenido[0] + "-Buscando", contenido[0] + "-Pujando"));
                            System.out.println("\t\t\tEl siguiente libro está disponible: " + contenido[0]);
                        }
                    } else {
                        bucle = false;
                    }
                }

                try {
                    for (String key : pujas.keySet()) {
                        if (pujas.get(key) <= libros.get(key)) {
                            ACLMessage pujando = new ACLMessage(ACLMessage.PROPOSE);
                            pujando.addReceiver(vendedor);
                            pujando.setContent(key + ";" + pujas.get(key));
                            myAgent.send(pujando);
                        } else {
                            gui.jTextArea1.setText(gui.jTextArea1.getText().replace(key + "-Pujando", key + "-Puja insuficiente"));
                            pujas.remove(key);
                            libros.remove(key);
                        }
                    }
                } catch (NullPointerException a) {}

                for (String key : noDisponibles.keySet()) {
                    ACLMessage busqueda = new ACLMessage(jade.lang.acl.ACLMessage.REQUEST);
                    busqueda.addReceiver(vendedor);
                    busqueda.setContent(key);
                    myAgent.send(busqueda);
                    //gui.jTextArea1.setText(gui.jTextArea1.getText().replace(key + "-Pujando", key + "-Puja Perdida"));
                    System.out.println("\t\t\tMensaje enviado para el libro: " + key);
                }
            }
        });
        System.out.println("\t\t\tVamos a hacer el doDelete");
    }

    public void takeDown() {
        System.out.println("AdiosCliente");
    }

    public class Buscador extends CyclicBehaviour {

        public void action() {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("vendedor");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                if (result != null) {
                    vendedor = result[0].getName();
                } else {
                    System.out.println("No hay subastadores");
                    block();
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
                block();
            }
            System.out.println("Vendedor encontrado");
            block();
        }
    }
}
