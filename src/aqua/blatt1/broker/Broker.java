package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;

import java.net.InetSocketAddress;

public class Broker {
    public static void main(String[] args) {
        Broker broker = new Broker();
        broker.broker();
    }

    Endpoint endpoint;
    ClientCollection clientCollection;
    int counter = 0;

    public Broker() {
        endpoint = new Endpoint(4711);
        clientCollection = new ClientCollection();
    }

    public void broker() {
        while (true) {
            Message msg = endpoint.blockingReceive();

            if (msg.getPayload() instanceof RegisterRequest) {
                register(msg);
            }

            if (msg.getPayload() instanceof DeregisterRequest) {
                deregister(msg);
            }

            if (msg.getPayload() instanceof HandoffRequest) {
                handoffFish(msg);
            }
        }
    }

    public void register(Message msg) {
        String id = "tank" + counter;
        counter++;
//      add tank to ClientCollection
        clientCollection.add(id, msg.getSender());
//      send message
        endpoint.send(msg.getSender(), new RegisterResponse(id));
    }

    public void deregister(Message msg) {
//      remove tank from list
        clientCollection.remove(clientCollection.indexOf(((DeregisterRequest) msg.getPayload()).getId()));
    }

    public void handoffFish(Message msg) {
        Direction direction = ((HandoffRequest) msg.getPayload()).getFish().getDirection();
        InetSocketAddress receiverAddress;
        int index = clientCollection.indexOf(msg.getSender());
        if (direction == Direction.LEFT) {
            receiverAddress = (InetSocketAddress) clientCollection.getLeftNeighorOf(index);
        } else {
            receiverAddress = (InetSocketAddress) clientCollection.getRightNeighorOf(index);
        }

        endpoint.send(receiverAddress, msg.getPayload());
    }
}