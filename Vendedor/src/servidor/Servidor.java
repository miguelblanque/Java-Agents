
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
import java.util.ArrayList;
import java.util.HashMap;

public class Servidor extends Agent {

    public HashMap<String, Puja> libros;

    protected void setup() {
        GUIServer gui = new GUIServer(this);
        gui.setVisible(true);
        gui.setTitle("Vendedor");

        System.out.println("Iniciando el servidor");
        libros = new HashMap<String, Puja>();

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("vendedor");
        sd.setName("vendedor");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        System.out.println("Servidor subido a páginas amarillas");

        addBehaviour(new RespuestaConsulta());
        System.out.println("Añadido comportamiento");

        addBehaviour(new TickerBehaviour(this, 10000) {
            protected void onTick() {
                System.out.println("Empezamos un tick");
                boolean bucle = true;
                for (String key : libros.keySet()) {
                    libros.get(key).participantes = new ArrayList<AID>();
                }

                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL);
                MessageTemplate mt2 = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
                while (bucle) {
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
                        String contenido[] = msg.getContent().split(";");
                        Puja aux = libros.get(contenido[0]);
                        aux.participantes.remove(msg.getSender());
                        aux.ganador = aux.participantes.get(0);
                    } else {
                        bucle = false;
                    }
                }
                bucle = true;

                while (bucle) {
                    ACLMessage msg = myAgent.receive(mt2);
                    if (msg != null) {
                        String contenido[] = msg.getContent().split(";");
                        System.out.println("Apuesta de " + msg.getSender().getName()
                                + " de " + contenido[1] + " para el libro " + contenido[0]);
                        Puja aux = libros.get(contenido[0]);
                        if (aux != null) {
                            int nVal = aux.precio + 1;
                            if (aux.precio == Integer.parseInt(contenido[1])) {
                                if (aux.participantes.size() == 0) {
                                    aux.ganador = msg.getSender();
                                    System.out.println(aux.nombre);
                                    gui.jTextArea1.setText(gui.jTextArea1.getText().replace(aux.nombre, aux.nombre + "-Pujando"));
                                } else {
                                    aux.precio = nVal;
                                    ACLMessage respuesta = msg.createReply();
                                    respuesta.setPerformative(ACLMessage.CFP);
                                    respuesta.setContent(contenido[0] + ";" + String.valueOf(nVal));
                                    myAgent.send(respuesta);
                                }
                                aux.participantes.add(msg.getSender());
                            } else {
                                ACLMessage respuesta = msg.createReply();
                                respuesta.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                respuesta.setContent(contenido[0]);
                                myAgent.send(respuesta);
                            }
                        } else {
                            ACLMessage respuesta = msg.createReply();
                            respuesta.setPerformative(ACLMessage.REJECT_PROPOSAL);
                            respuesta.setContent(contenido[0]);
                            myAgent.send(respuesta);
                        }
                    } else {
                        bucle = false;
                    }
                }

                for (String key : libros.keySet()) {
                    if (libros.get(key).participantes.size() == 1) {
                        ACLMessage ganador = new ACLMessage(ACLMessage.INFORM);
                        ganador.addReceiver(libros.get(key).ganador);
                        ganador.setContent(libros.get(key).nombre);
                        libros.remove(key);
                        myAgent.send(ganador);
                        System.out.println("Mensajes a ganador mandado");
                        gui.jTextArea1.setText(gui.jTextArea1.getText().replace(key+"-Pujando\n",""));
                    } else {
                        ACLMessage seguimos = new ACLMessage(ACLMessage.CFP);
                        seguimos.addReceiver(libros.get(key).ganador);
                        seguimos.setContent(libros.get(key).nombre + ";"
                                + String.valueOf(libros.get(key).precio));
                        myAgent.send(seguimos);
                    }
                }
            }
        });
        System.out.println("Vamos a hacer el doDelete");
    }

    protected void takeDown() {
        System.out.println("Voy a hacer el takedown");
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("AdiosServidor");
    }

    public class RespuestaConsulta extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            boolean bucle = true;
            while (bucle) {
                ACLMessage msg = myAgent.receive(mt);
                if (msg != null) {
                    String libro = msg.getContent();
                    ACLMessage reply = msg.createReply();

                    Puja subasta = libros.get(libro);
                    if (subasta != null) {
                        reply.setPerformative(ACLMessage.CFP);
                        reply.setContent(subasta.nombre + ";" + String.valueOf(subasta.precio));
                    } else {
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent("NULL");
                    }
                    myAgent.send(reply);
                    System.out.println("Se ha atendido a una consulta");
                }else{
                    bucle=false;
                }
            }
            block();
        }
    }
}
