package parkify;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.awt.print.Book;
import java.util.Map;
import java.util.HashMap;

public class StationAgent extends Agent {

    private Map<String, String> placesStatus;
    private  String address = "@192.168.1.23:1099/JADE";

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
            placesStatus = new HashMap<String, String>();
            WaitForCarPlateBehaviour waitForCarPlateBehaviour = new WaitForCarPlateBehaviour(this);
            addBehaviour(waitForCarPlateBehaviour);
            WaitForPlaceStatusBehaviour waitForPlaceStatusBehaviour = new WaitForPlaceStatusBehaviour(this);
            addBehaviour(waitForPlaceStatusBehaviour);
            HandleReservationBehaviour handleReservationBehaviour = new HandleReservationBehaviour(this);
            addBehaviour(handleReservationBehaviour);
            HandleGetFreePlacesBehaviour handleGetFreePlacesBehaviour = new HandleGetFreePlacesBehaviour(this);
            addBehaviour(handleGetFreePlacesBehaviour);



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
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("car-plate-status"),
                    MessageTemplate.MatchPerformative( ACLMessage.INFORM ));
            ACLMessage msg = receive(mt);

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
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("place-status"),
                    MessageTemplate.MatchPerformative( ACLMessage.INFORM ));
            ACLMessage msg = receive(mt);

            if(msg != null && msg.getPerformative()== ACLMessage.INFORM) {
                String content = msg.getContent();

                if ((content != null) && !(content.startsWith("car"))){
                    placesStatus.put(msg.getSender().getName(), content);

                    System.out.println("Agent " + getLocalName()+ " get parking place status: " + content +
                            " from " + msg.getSender().getName());

                }
            } else {
                block();
            }
        }
    }

    /**
     * Odbiera od aplikacji klienckiej prośbę o rezerwację miejsca i
     * wysyla żądanie do agenta miejsca parkingowego o rezerwację miejsca.
     * Generowanie prośby jest symulowane w GUI poprzez wysłanie odpowiedniej wiadomości.
     */
    private class HandleReservationBehaviour extends CyclicBehaviour{
        HandleReservationBehaviour(Agent a) { super(a);}

        @Override
        public void action(){
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("place-reservation"),
                    MessageTemplate.MatchPerformative( ACLMessage.REQUEST ));
            ACLMessage msgReservationFromApp = receive(mt);

            if(msgReservationFromApp != null) {
                String content = msgReservationFromApp.getContent(); //eg. "firstParkingAgent"
                String parkingPlaceID = content + address;
                System.out.println("ParkingplaceID = " + parkingPlaceID);
                System.out.println("STATUS = " + placesStatus.get(parkingPlaceID));

                if(placesStatus.get(parkingPlaceID).equals("false")){
                    System.out.println("Place is free");
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setConversationId("place-reservation");
                    msg.addReceiver(new AID(parkingPlaceID, AID.ISGUID));
                    msg.setContent("RESERVE");
                    send(msg);
                }
                else{
                    System.out.println("Place is taken");
                }

            } else {
                block();
            }

        }
    }

    /**
     * Odbiera od aplikacji klienckiej zapytanie o status miejsc i prezentuje ich status.
     * Generowanie zapytania jest symulowane w GUI poprzez wysłanie odpowiedniej wiadomości.
     */
    private class HandleGetFreePlacesBehaviour extends CyclicBehaviour{
        HandleGetFreePlacesBehaviour(Agent a) { super(a);}

        @Override
        public void action(){
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("free-places-status"),
                    MessageTemplate.MatchPerformative( ACLMessage.REQUEST ));
            ACLMessage msgFreePlacesRequest = receive(mt);

            if(msgFreePlacesRequest != null) {
                String content = msgFreePlacesRequest.getContent(); //eg. "getPlacesStatus"

                if(content!=null&&content.startsWith("getPlacesStatus")){
                    System.out.println("Current status of parking places: ");
                    for (Map.Entry<String, String> entry : placesStatus.entrySet())
                    {
                        if(entry.getValue().equals("false")){
                            System.out.println("Place " + entry.getKey() + " is FREE");
                        }
                        else {
                            System.out.println("Place " + entry.getKey() + " is TAKEN");

                        }
                    }

                }

            } else {
                block();
            }

        }
    }
}
