package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;

import javax.swing.*;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
    // TODO: ClientList wird ebenfalls von verschiedenen Threads genutzt -> Änderungen mpssen aber nicht sofort sichtbar sein, daher ist Volatile nicht notwendig, richtig? -> inkonsistenz wird mit synchronized gewährleistet
    private ClientCollection clientList;
    private int numThreads = 5;
    private ExecutorService executor;
    private ReadWriteLock lock;
    private volatile boolean stopRequested;

    public static void main(String[] args) {
        Broker broker = new Broker();
        broker.broker();
    }

    public Broker() {
        this.endpoint = new Endpoint(4711);
        this.clientList = new ClientCollection();
        this.executor = Executors.newFixedThreadPool(numThreads);
        this.lock = new ReentrantReadWriteLock();
    }

    public void broker() {
        while(true){
            Message msg = endpoint.blockingReceive();
            if (msg.getPayload() instanceof PoisonPill){
                break;
            }
            BrokerTask task = new BrokerTask();
            executor.execute(() -> task.brokerTask(msg));
        }
        executor.shutdown();
    }

    public class BrokerTask {
        // inner class - Verarbeitung und Beantwortung von Nachrichten

        public void brokerTask(Message msg) {
            if (msg.getPayload() instanceof RegisterRequest){
                synchronized (clientList){
                    register(msg);
                }
            }

            if (msg.getPayload() instanceof DeregisterRequest){
                synchronized (clientList){
                    deregister(msg);
                }
            }

            if (msg.getPayload() instanceof HandoffRequest){
                //TODO: Fragen -> richtiger Lock? -> in handoff wird nichts geschrieben
                lock.readLock().lock();
                handoffFish(msg);
                lock.readLock().unlock();
            }

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