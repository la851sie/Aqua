package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.*;
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

    //TODO: Token kann verloren gehen....
    private Endpoint endpoint;
   // Wann volatile ? -> bei primitiven Datentypen -> optimierungsproblem des Compilers vermeiden -> andere datentypen schützt man mit synchronisations, locks etc.
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
            BrokerTask task = new BrokerTask(msg);
            executor.execute(task);

            // executer.execute(() -> task.brokerTask(msg)); -> War teil meiner ersten Lösung
            // was passiert hier?
            // Lambda Expression -> executer erwartet Runnable (=Interface) -> methode "task.brokerTask(msg)" wird in run-methode aus Interface eingebaut.
            // Warum wird der Übergabgeparameter nicht in der erstn () deklariert?
            // -> nur für unbekannte variablen, bekannte Variablen (sieh msg) kann in der Methode übergeben werden, da auf bereits zugegriffen werden kann - logisch....
        }
        executor.shutdown();
    }

    public class BrokerTask implements Runnable {

        private Message msg;

        public BrokerTask(Message msg) {
            this.msg = msg;
        }

        public void run() {
            if (msg.getPayload() instanceof RegisterRequest) {
                synchronized (clientList) {
                    register(msg);
                }
            }

            if (msg.getPayload() instanceof DeregisterRequest) {
                synchronized (clientList) {
                    deregister(msg);
                }
            }

            if (msg.getPayload() instanceof HandoffRequest) {
                // Read Lock, da im Handoff nur gelesen wird -> so wird sichergestellt, dass mehrere HandoffRequests gleichzeitig bearbeitet werden können.
                // Register/Deregister schreiben auch, daher wurde das mit "synchronized" realisiert
                lock.readLock().lock();
                handoffFish(msg);
                lock.readLock().unlock();
            }

            if (msg.getPayload() instanceof NameResolutionRequest) {
                String tankId = ((NameResolutionRequest) msg.getPayload()).getSearchedTankId();
                String requestId = ((NameResolutionRequest) msg.getPayload()).getRequestId();
                InetSocketAddress tankAddress = (InetSocketAddress) clientList.getClient(clientList.indexOf(tankId));
                endpoint.send(msg.getSender(), new NameResolutionResponse(tankAddress, requestId, msg.getSender()));
                // TODO: Warum brauche ich hier noch die Senderadresse?
            }
        }
    }

    public void register(Message msg) {
        InetSocketAddress sender = msg.getSender();
        String id = "tank"+(clientList.size());
        clientList.add(id, sender);
        System.out.println("Added id:"+ id +" to the list");

        RegisterResponse response = new RegisterResponse(id);
        if(clientList.size() == 1){
            endpoint.send(sender,new Token());
        }
        endpoint.send(sender, response);

        // Neuer Tank, neuer Linker nachbar, neuer rechter Nachbar müssen update zu Ihren Nachbarn bekommen
        InetSocketAddress rightNeighbor = (InetSocketAddress) clientList.getRightNeighorOf(clientList.indexOf(sender));
        InetSocketAddress leftNeighbor = (InetSocketAddress) clientList.getLeftNeighorOf(clientList.indexOf(sender));

        updateNeighborOf(sender);
        if (clientList.size() > 1){
            updateNeighborOf(rightNeighbor);
            updateNeighborOf(leftNeighbor);
        }
    }

    public void updateNeighborOf(InetSocketAddress tank){
        InetSocketAddress leftNeighbor = (InetSocketAddress) clientList.getLeftNeighorOf(clientList.indexOf(tank));
        InetSocketAddress rightNeighbor = (InetSocketAddress) clientList.getRightNeighorOf(clientList.indexOf(tank));
        endpoint.send(tank, new NeighborUpdate(leftNeighbor,rightNeighbor));
    }

    public void deregister(Message msg) {
        InetSocketAddress sender = msg.getSender();
        int i = clientList.indexOf(sender);

        // linker und rechter nachbar herrausfinden
        InetSocketAddress rightNeighbor = (InetSocketAddress) clientList.getRightNeighorOf(clientList.indexOf(sender));
        InetSocketAddress leftNeighbor = (InetSocketAddress) clientList.getLeftNeighorOf(clientList.indexOf(sender));

        // Sender entfernen
        clientList.remove(i);
        System.out.println("Removed id: tank"+ i +" from the list");

        if(clientList.size() != 0){
            // Ehemalige Nachbarn updaten
            updateNeighborOf(rightNeighbor);
            updateNeighborOf(leftNeighbor);
        }
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
