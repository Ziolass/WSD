package parkify;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Random;

public class CameraAgent extends Agent {

    // Agent stacji powiazany z tym agentem kamery
    private final String msgReciver = "firstStationAgent@192.168.1.23:1099/JADE";

    @Override
    protected void setup() {
        Random rand = new Random(123456);
        System.out.println("Agent " + getLocalName()+ " started.");

        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("CameraAgent");
        sd.setName(getName());
        sd.setOwnership("PARKIFY");
        dfd.setName(getAID());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            /**
             * Ten agent co 5 sekund wysyła informacje o tym, ze
             * zobaczył samochod
             */
            addBehaviour(new TickerBehaviour(this, 5000) {
                protected void onTick() {
                    int random_integer = rand.nextInt(99999 - 10000) + 10000;

                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.setConversationId("car-plate-status");
                    msg.setContent("car" + random_integer);
                    msg.addReceiver(new AID(msgReciver, AID.ISGUID));
                    send(msg);

                    System.out.println("Agent " + myAgent.getLocalName() + " send message: car" + random_integer);
                }
            });
        } catch (Exception e ) {
            System.out.println("Agent " + getLocalName()+ " not started."+ e.getStackTrace());
            doDelete();
        }
    }
}

