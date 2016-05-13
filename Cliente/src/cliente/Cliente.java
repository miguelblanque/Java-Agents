
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ConcurrentModificationException;
import java.util.HashMap;

public class Cliente extends Agent {

    public AID vendedor;
    public HashMap<String, DataContainer> libros;
    public HashMap<String, DataContainer> pujas;
    public HashMap<String, DataContainer> noDisponibles;
    public HashMap<String, DataContainer> salida;

    public void setup() {
        Principal gui = new Principal(this);
        gui.setVisible(true);
        gui.setTitle("Cliente");
        vendedor = null;

        libros = new HashMap<String, DataContainer>();
        pujas = new HashMap<String, DataContainer>();
        noDisponibles = new HashMap<String, DataContainer>();
        salida = new HashMap<String, DataContainer>();

        addBehaviour(new TickerBehaviour(this, 2000) {
            protected void onTick() {
                if (vendedor == null) {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("vendedor");
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        if (result.length > 0) {
                            vendedor = result[0].getName();
                        } else {
                            block();
                        }
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                        block();
                    }
                    block();
                } else {
                    block();
                }
            }
        });
        System.out.println("Añadido primer comportamiento");

        addBehaviour(new TickerBehaviour(this, 5000) {
            @Override
            protected void onTick() {
                if (vendedor != null) {
                    for (String key : salida.keySet()) {
                        ACLMessage msg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                        msg.setSender(vendedor);
                        msg.setContent(key);
                        myAgent.send(msg);
                        salida.remove(key);
                    }

                    MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REFUSE);
                    boolean bucle = true;
                    while (bucle) {
                        ACLMessage msg = myAgent.receive(mt);
                        if (msg != null) {
                            System.out.println(msg.getContent());
                            System.out.println("Hola");
                            noDisponibles.get(msg.getContent()).flag=0;
                        } else {
                            bucle = false;
                        }
                    }

                    mt = MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL);
                    bucle = true;
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
                                DataContainer a = new DataContainer(0, Integer.parseInt(contenido[1]));
                                pujas.replace(contenido[0], a);
                            } else {
                                DataContainer a = new DataContainer(0, (Integer.parseInt(contenido[1]) + 1));
                                pujas.put(contenido[0], a);
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
                            if (pujas.get(key).precio <= libros.get(key).precio) {
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
                    } catch (ConcurrentModificationException a) {
                    }

                    for (String key : noDisponibles.keySet()) {
                        if (noDisponibles.get(key).flag == 0) {
                            ACLMessage busqueda = new ACLMessage(jade.lang.acl.ACLMessage.REQUEST);
                            busqueda.addReceiver(vendedor);
                            busqueda.setContent(key);
                            myAgent.send(busqueda);
                            noDisponibles.get(key).flag = 1;
                            System.out.println("\t\t\tMensaje enviado para el libro: " + key);
                        }
                        //gui.jTextArea1.setText(gui.jTextArea1.getText().replace(key + "-Pujando", key + "-Puja Perdida"));
                    }
                }
            }
        });
    }

    public void takeDown() {
        System.out.println("AdiosCliente");
    }
}
