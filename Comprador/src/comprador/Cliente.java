import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Cliente extends Agent {
    public HashMap<String, Integer> libros;
    public ArrayList<String> limpieza;
    public AID[] subastadores;
    public Principal gui;
    public int i;

    protected void setup() {
        libros = new HashMap();
        limpieza = new ArrayList<String>();
        gui = new Principal(this);
        gui.setTitle("Comprador");
        gui.setVisible(true);
        gui.jTextArea1.setEditable(false);
        i=0;

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setName("puja");
        sd.setType("puja");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fipa) {
            fipa.printStackTrace();
        }
        System.out.println("\t\t\tMe he registrado en el servicio");
        addBehaviour(new comportamiento());
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fipa) {
            fipa.printStackTrace();
        }
    }

    private class comportamiento extends CyclicBehaviour {

        public void action() {
            gui.setTitle(myAgent.getName());
            MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.CFP), MessageTemplate.MatchPerformative(ACLMessage.INFORM)));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                //System.out.println("\t\t\tEmpezamos");
                String contenido[] = msg.getContent().split(";");
                if (msg.getPerformative() == ACLMessage.CFP) {
                    ACLMessage respuesta = msg.createReply();
                    if (libros.containsKey(contenido[0])) { //Si estamos pujando por el libro
                        if (Integer.parseInt(contenido[1]) <= libros.get(contenido[0])) { //Si el precio de puja es menor que nuestro max
                            respuesta.setContent(contenido[0] + ";" + contenido[1]);
                            respuesta.setPerformative(ACLMessage.PROPOSE);
                            myAgent.send(respuesta);
                            System.out.println("\t\t\t" + myAgent.getAID().getName() + ": " + contenido[1] + " por " + contenido[0]);
                            gui.jTextArea1.setText(gui.jTextArea1.getText().replace(contenido[0] + " - Buscando", contenido[0] + " - Pujando"));
                        } else { //Si no podemos seguir pujando nos retiramos
                            respuesta.setConversationId("BAJA");
                            respuesta.setContent(contenido[0] + ";" + contenido[1]);
                            respuesta.setPerformative(ACLMessage.REFUSE);
                            myAgent.send(respuesta);
                            gui.jTextArea1.setText(gui.jTextArea1.getText().replace(contenido[0] + " - Pujando", contenido[0] + " - Puja insuficiente"));
                            //System.out.println("\t\t\tNo puedo pujar por "+contenido[0]);
                        }
                    } else { //Si no estamos pujando por el libro
                        respuesta.setContent(contenido[0] + ";" + contenido[1]);
                        respuesta.setPerformative(ACLMessage.REFUSE);
                        myAgent.send(respuesta);
                        //System.out.println("\t\t\tNo quiero pujar por "+contenido[0]);
                    }
                } else if (msg.getPerformative() == ACLMessage.REQUEST) { //Subasta ganada
                    if (libros.containsKey(contenido[0])) {
                        libros.remove(contenido[0]);
                        gui.jTextArea1.setText(gui.jTextArea1.getText().replace(contenido[0] + " - Pujando", contenido[0] + " - Ganado"));
                        System.out.println("\t\t\tHe ganado el libro: " + contenido[0] + " por " + contenido[1]);
                    } else {
                        System.out.println("\t\t\tNo se ha podido eliminar el libro: " + contenido[0]);
                    }
                } else if (msg.getPerformative() == ACLMessage.INFORM) {
                    if (msg.getConversationId().equals("FINRONDA")) {
                        if (contenido[2].equals(myAgent.getName())) {
                            //System.out.println("\t\t\tHe ganado la ronda actual por el libro "+contenido[0]);
                        } else {
                            //System.out.println("\t\t\tNo he ganado la ronda actual por el libro "+contenido[0]);
                        }
                    } else if (msg.getConversationId().equals("BAJA")) {
                        gui.jTextArea1.setText(gui.jTextArea1.getText().replace(contenido[0] + " - Pujando", contenido[0] + " - Retirado"));
                        System.out.println("\t\t\tMe he dado de baja del libro " + contenido[0]);
                    }
                }
            } else {
                for(String aux:limpieza){
                    libros.remove(aux);
                }
                limpieza = new ArrayList<String>();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    System.out.println("Ups");
                    Logger.getLogger(Cliente.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
