package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;

import java.net.InetSocketAddress;

/*
 * LASI:
 * Alle Klienten kommunizieren in der initialen Version nur mit dem Broker.
 * Wenn ein Fich ein Aquarium verlässt meldet das Aquarium (Client) dem Broker einen Handoff-Request. Dieser wird durch den Broker
 * an das linke bzw. rechte Aquarium (Client) weitergeleitet.
 *
 * Parameters:
 * clientList = Liste angemeldeter Clienten
 * Endpoint = Nachrichtenbasierte Senden und Erhalten Funktionalitäten
 */

public class Broker {
    private Endpoint endpoint;
    private ClientCollection clientList;


    public static void main(String[] args) {
        Broker broker = new Broker();
        broker.runBroker();
    }

    public Broker() {
        this.endpoint = new Endpoint(4711);
        this.clientList = new ClientCollection();
    }


    // Aufgabe 1
    public void runBroker() {
        while (true) {
            Message msg = endpoint.blockingReceive();

            if (msg.getPayload() instanceof RegisterRequest)
                register(msg);

            if (msg.getPayload() instanceof DeregisterRequest)
                deregister(msg);

            if (msg.getPayload() instanceof HandoffRequest)
                handoffFish(msg);

        }
    }

    public void register(Message msg) {
        InetSocketAddress sender = msg.getSender();
        String id = "tank"+(clientList.size());
        clientList.add(id, sender);
        System.out.println("Added id:"+ id +" to the list");

        RegisterResponse response = new RegisterResponse(id);
        endpoint.send(sender, response);
    }

    public void deregister(Message msg) {
        InetSocketAddress sender = msg.getSender();
        int i = clientList.indexOf(sender);
        clientList.remove(i);
        System.out.println("Removed id: tank"+ i +" from the list");
    }

    public void handoffFish(Message msg) {
        // Sender rausfinden
        InetSocketAddress sender = msg.getSender();
        InetSocketAddress neighor;
        int i = clientList.indexOf(sender);

        // Nachbar rausfinden
        HandoffRequest handoffRequest = (HandoffRequest) msg.getPayload();
        FishModel fish = handoffRequest.getFish();
        Direction direction = fish.getDirection();
        // -1 = left | +1 = right
        if (direction == Direction.LEFT) {
            neighor = (InetSocketAddress) clientList.getLeftNeighorOf(i);
        }else {
            neighor = (InetSocketAddress) clientList.getRightNeighorOf(i);
        }

        // HandoffRequest weitergeben
        endpoint.send(neighor, new HandoffRequest(fish));
        System.out.println("Fisch gesendet");
    }
}