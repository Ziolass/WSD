package parkify;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class StationAgent extends Agent {

    @Override
    protected void setup() {
        System.out.println("Agent " + getLocalName()+ " started.");

        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("StationAgent");
        sd.setName(getName());
        sd.setOwnership("PARKIFY");
        dfd.setName(getAID());
        dfd.addServices(sd);

        try {
            DFService.register(this,dfd);
            WaitForCarPlateBehaviour waitForCarPlateBehaviour = new WaitForCarPlateBehaviour(this);
            addBehaviour(waitForCarPlateBehaviour);
            WaitForPlaceStatusBehaviour waitForPlaceStatusBehaviour = new WaitForPlaceStatusBehaviour(this);
            addBehaviour(waitForPlaceStatusBehaviour);

        } catch (Exception e) {
            System.out.println("Agent " + getLocalName()+ " not started."+ e.getStackTrace());
            doDelete();
        }
    }

    /**
     * Czeka na informację od agenta kamery o zauwazonym samochodzie i
     * sprawdza czy samochod zaparowal legalnie.
     * Jezeli nie to wysyla informacje na policje (25% szans na nielegalne parkowanie)
     */
    private class WaitForCarPlateBehaviour extends CyclicBehaviour {

        WaitForCarPlateBehaviour(Agent a) {
            super(a);
        }

        @Override
        public void action() {
            ACLMessage msg = receive(MessageTemplate.MatchPerformative( ACLMessage.INFORM ));

            if(msg != null && msg.getPerformative()== ACLMessage.INFORM) {
                String content = msg.getContent();

                if ((content != null) && content.startsWith("car") && (Integer.parseInt(content.substring(3)) % 4) == 0){
                    System.out.println("Agent " + getLocalName()+ " send report to police with plate: " + content);

                } else {
                    System.out.println("Agent " + getLocalName()+ " authorize care plate: " + content);
                }

            } else {
                block();
            }
        }
    }

    /**
     * Czeka na informację od agenta miejsca parkingowego o zajetosci miejsca
     */
    private class WaitForPlaceStatusBehaviour extends CyclicBehaviour {

        WaitForPlaceStatusBehaviour(Agent a) {
            super(a);
        }

        @Override
        public void action() {
            ACLMessage msg = receive(MessageTemplate.MatchPerformative( ACLMessage.INFORM ));

            if(msg != null && msg.getPerformative()== ACLMessage.INFORM) {
                String content = msg.getContent();

                if ((content != null) && !(content.startsWith("car"))){
                    System.out.println("Agent " + getLocalName()+ " get parking place status: " + content +
                            " from " + msg.getSender().getName());

                }
            } else {
                block();
            }
        }
    }

    /**
     * Wysyla inforacje do agenta miejsca parkingowego o rezeracji miejsca
     */
//    private class ... extends ... { }
}
