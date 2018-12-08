package parkify;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Random;

public class ParkingAgent extends Agent {

    // Agent stacji powiazany z tym agentem miejsca parkingowego
    private final String msgReciver = "firstStationAgent@192.168.1.23:1099/JADE";

    @Override
    protected void setup() {
        Random rand = new Random(123456);
        System.out.println("Agent " + getLocalName()+ " started.");

        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("ParkingAgent");
        sd.setName(getName());
        sd.setOwnership("PARKIFY");
        dfd.setName(getAID());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            addBehaviour(new CheckPlaceStatusBehaviour(this, 10000));
            addBehaviour(new WaitForReservePlaceBehaviour(this));

        } catch (Exception e ) {
            System.out.println("Agent " + getLocalName()+ " not started."+ e.getStackTrace());
            doDelete();
        }
    }

    /**
     * Sprawdza co 10 sekund czy miejsce parkingowe jest zajete i
     * wysyla do powiazanej stacji true - zajete lub false - wolne
     * (szanse: 50% zajete miejsce, 50% wolne miejsce)
     */
    private class CheckPlaceStatusBehaviour extends TickerBehaviour {
        CheckPlaceStatusBehaviour(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            Random rand = new Random();

            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setConversationId("place-status");
            if(rand.nextFloat() > 0.5) {
                msg.setContent("true");

            } else {
                msg.setContent("false");
            }

            msg.addReceiver(new AID(msgReciver, AID.ISGUID));
            send(msg);

            System.out.println("Agent " + myAgent.getLocalName() + " send if parking place taken: " + msg.getContent());
        }
    }

    /**
     * Czeka na wiadomocs od agenta stacji o rezerwacji miejsca
     */
    private class WaitForReservePlaceBehaviour extends CyclicBehaviour {

        WaitForReservePlaceBehaviour(Agent a) {
            super(a);
        }

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("place-reservation"),
                    MessageTemplate.MatchPerformative( ACLMessage.REQUEST ));
            ACLMessage msg = receive(mt);

            if(msg != null) {
                String content = msg.getContent();

                if ((content != null) && content.startsWith("RESERVE")){
                    System.out.println("Agent " + getLocalName()+ " reserved parking place.");
                }
            } else {
                block();
            }
        }
    }
}
