import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;
import java.util.HashMap;

public class Vendedor extends Agent {
    public HashMap<String, DataContainer> libros;
    public ArrayList<AID> participantes;
    public GUIServer gui;
    
    protected void setup() {
        libros = new HashMap<String, DataContainer>();
        gui = new GUIServer(this);
        gui.setTitle("Vendedor");
        gui.setVisible(true);
        gui.jTextArea1.setEditable(false);
        
        addBehaviour(new TickerBehaviour(this, 10000) {
            protected void onTick() {
                ArrayList<String> basura = new ArrayList<String>();
                for (String nombre : libros.keySet()) {
                    DataContainer dc = libros.get(nombre);
                    if (dc.participantes.size() <= 1) {
                        if (dc.ganador != null) {
                            ACLMessage finalizado = new ACLMessage(ACLMessage.REQUEST);
                            finalizado.setContent(nombre + ";" + (dc.precio - dc.incremento));
                            finalizado.addReceiver(dc.ganador);
                            myAgent.send(finalizado);
                            basura.add(nombre);
                            gui.jTextArea1.setText(gui.jTextArea1.getText().replace(nombre + " - a la venta", nombre + " - Vendido"));
                        }
                    }
                }
                for (String nombre : basura) {
                    libros.remove(nombre);
                }
                int i = 0;
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("puja");
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    participantes = new ArrayList<AID>();
                    //System.out.println("He encontrado "+result.length+" compradores");
                    for (i = 0; i < result.length; i++) {
                        participantes.add(result[i].getName());
                    }
                    if (libros.size() > 0) {
                        myAgent.addBehaviour(new Controlador());
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
            }
        });
    }
    
    protected void takeDown() {
        System.out.println("Adios!");
    }
    
    private class Controlador extends Behaviour {
        
        public int respuestas = 0;
        public int paso = 0;
        public MessageTemplate mt;
        
        public void action() {
            int i;
            //System.out.println("Controlador iniciado");
            switch (paso) {
                case 0:
                    for (String aux : libros.keySet()) {
                        DataContainer dc = libros.get(aux);
                        ACLMessage msg = new ACLMessage(ACLMessage.CFP);
                        for (i = 0; i < participantes.size(); i++) {
                            msg.addReceiver(participantes.get(i));
                        }
                        msg.setReplyWith("cfp" + System.currentTimeMillis());
                        msg.setConversationId("CFP");
                        msg.setContent(aux + ";" + dc.precio);
                        myAgent.send(msg);
                        //System.out.println("Pedida de puja para el libro " + aux);
                    }
                    mt = MessageTemplate.or(MessageTemplate.MatchConversationId("CFP"), MessageTemplate.MatchConversationId("BAJA"));
                    paso = 1;
                    break;
                
                case 1:
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
                        String contenido[] = msg.getContent().split(";");
                        DataContainer dc = libros.get(contenido[0]);
                        if (msg.getPerformative() == ACLMessage.PROPOSE) {
                            ArrayList<AID> tmp = dc.participantes;
                            if (!(dc.participantes.contains(msg.getSender()))) {
                                dc.participantes.add(msg.getSender());
                                System.out.println("Pujador puesto en participantes por el libro " + dc.nombre);
                            }
                            if (Integer.parseInt(contenido[1]) == dc.precio) {
                                dc.precio = dc.precio + dc.incremento;
                                dc.ganador = msg.getSender();
                                //System.out.println("Recibida puja ganadora de " + msg.getSender().getName() + " para el libro " + dc.nombre);
                            } else {
                                //System.out.println("Recibida puja no ganadora de " + msg.getSender().getName() + " para el libro " + dc.nombre);
                            }
                            ACLMessage info = new ACLMessage(ACLMessage.INFORM);
                            info.setConversationId("FINRONDA");
                            info.setContent(contenido[0] + ";" + (dc.precio - dc.incremento) + ";" + dc.ganador.getName());
                            info.addReceiver(msg.getSender());
                            myAgent.send(info);
                            //System.out.println("Mensaje de fin de ronda enviado para " + msg.getSender().getName());
                        } else if (msg.getPerformative() == ACLMessage.REFUSE) {
                            if (dc.participantes.contains(msg.getSender())) {
                                dc.participantes.remove(msg.getSender());
                                if (msg.getConversationId().equals("BAJA")) {
                                    System.out.println("Pujador dado de baja para el libro " + contenido[0]);
                                    ACLMessage info = new ACLMessage(ACLMessage.INFORM);
                                    info.setContent(contenido[0] + ';' + contenido[1]);
                                    info.addReceiver(msg.getSender());
                                    info.setConversationId("BAJA");
                                    myAgent.send(info);
                                }
                            }
                        }
                        respuestas++;
                        if (respuestas >= (participantes.size() * libros.size())) {
                            paso = 2;
                        }
                    } else {
                        block();
                    }
                    break;
            }
        }
        
        public boolean done() {
            if (paso == 2) {
                return true;
            } else {
                return false;
            }
        }
    }
}
