package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.RecorderState;
import aqua.blatt1.common.msgtypes.SnapshotCollector;
import aqua.blatt1.common.msgtypes.SnapshotMarker;
import aqua.blatt1.common.msgtypes.Token;

public class TankModel extends Observable implements Iterable<FishModel> {

	public static final int WIDTH = 600;
	public static final int HEIGHT = 350;
	protected static final int MAX_FISHIES = 5;
	protected static final Random rand = new Random();
	protected volatile String id;
	protected final Set<FishModel> fishies;
	protected int fishCounter = 0;
	protected final ClientCommunicator.ClientForwarder forwarder;

	public InetSocketAddress leftNeighbor;
	public InetSocketAddress rightNeighbor;
	protected boolean boolToken;
	Timer timer = new Timer();
	public int localState;
	public int globalValue;
	public RecorderState recorderState = RecorderState.IDLE;
	public boolean snapshotComplete = false;
	public boolean initiator = false;
	public boolean showDialog = false;
	public boolean hasCollector = false;
	ExecutorService executor = Executors.newFixedThreadPool(1);

	public TankModel(ClientCommunicator.ClientForwarder forwarder) {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.forwarder = forwarder;
	}

	synchronized void onRegistration(String id) {
		this.id = id;
		newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
	}

	public synchronized void newFish(int x, int y) {
		if (fishies.size() < MAX_FISHIES) {
			x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
			y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishies.add(fish);
		}
	}

	synchronized void receiveFish(FishModel fish) {
		if ((fish.getDirection() == Direction.LEFT && recorderState == RecorderState.RIGHT) || (fish.getDirection() == Direction.RIGHT && recorderState == RecorderState.LEFT) || recorderState == RecorderState.BOTH) {
			localState++;
		}

		fish.setToStart();
		fishies.add(fish);
	}

	public String getId() {
		return id;
	}

	public synchronized int getFishCounter() {
		return fishCounter;
	}

	public synchronized Iterator<FishModel> iterator() {
		return fishies.iterator();
	}

	private synchronized void updateFishies() {
		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();

			fish.update();

			if (fish.hitsEdge())
				if (hasToken()){
					forwarder.handOff(fish,leftNeighbor,rightNeighbor);
				}else{
					fish.reverse();
				}


			if (fish.disappears())
				it.remove();
		}
	}

	private synchronized void update() {
		updateFishies();
		setChanged();
		notifyObservers();
	}

	protected void run() {
		forwarder.register();

		try {
			while (!Thread.currentThread().isInterrupted()) {
				update();
				TimeUnit.MILLISECONDS.sleep(10);
			}
		} catch (InterruptedException consumed) {
			// allow method to terminate
		}
	}

	public synchronized void finish() {
		forwarder.deregister(id);
	}

	public synchronized void updateNeighbors(InetSocketAddress leftNeighbor, InetSocketAddress rightNeighbor) {
		this.leftNeighbor = leftNeighbor;
		this.rightNeighbor = rightNeighbor;
		System.out.println("Update Neighbors for " + id);
	}

	public void receiveToken(Token token) {
		boolToken = true;
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				boolToken = false;
				forwarder.sendToken(leftNeighbor, token);
			}
		}, 2000);
	}

	public boolean hasToken(){
		if (boolToken) {
			System.out.println("I am  " + id + " and have the token");
			return true;
		} else {
			return false;
		}
	}

	public void receiveSnapshotCollector(SnapshotCollector marker){
		if(!initiator){
			hasCollector = true;
			executor.execute(() -> {
				while (hasCollector) {
					if (snapshotComplete) {
						marker.addToSnapshot(localState);
						forwarder.sendSnapshotCollector(leftNeighbor, marker);
						hasCollector = false;
						snapshotComplete = false;
					}
				}
			});
		}else {
			initiator = false;
			globalValue = marker.getCounter();
			System.out.println(globalValue);
			showDialog = true;
			hasCollector = false;
		}
	}

	public void initiateSnapshot(){
		System.out.println("startet snapshot");
		//Speichere den Lokalen Zustand
		localState = fishies.size();
		initiator = true;
		forwarder.sendSnapshotCollector(leftNeighbor, new SnapshotCollector(localState));
		//starte Aufzeichnungsmodus für alle Eingangskanäle
		recorderState = RecorderState.BOTH;
		//Sende Markierungen in alle Ausgangskanäle
		forwarder.sendSnapshotMarker(leftNeighbor, new SnapshotMarker());
		forwarder.sendSnapshotMarker(rightNeighbor, new SnapshotMarker());
	}

	public void receiveSnapshotMarker(InetSocketAddress sender, SnapshotMarker marker){
		// Markierung geht über einen Eingangskanal c ein
		// falls sich P nicht im Aufzeichnungsmodus befindet:
		// 1) speichere lokalen Zustand,
		// 2) speichere den Zustand von c als leere Liste,
		// 3) starte Aufzeichnung für alle anderen Eingangskanäle
		// 4) sende Markierung in alle Ausgangskanäle

		//falls sich P nicht im Aufzeichnungsmodus befindet:
		if (recorderState == RecorderState.IDLE) {
			// 1) speichere lokalen Zustand
			localState = fishies.size();
			// 2) speichere den Zustand von c als leere Liste -> fällt weg (lokaler zustand wird über fische gespeichert
			// 3) starte Aufzeichnung für alle anderen Eingangskanäle

			//falls rechter = linker Nachbar
			if(leftNeighbor.equals(rightNeighbor)){
				recorderState = RecorderState.BOTH;
			}
			if (sender.equals(leftNeighbor)) {
				// Eingangskanal von links ist bereits bedient
				recorderState = RecorderState.RIGHT;
			} else if (sender.equals(rightNeighbor)) {
				// Eingangskanal von rechts ist bereits bedient
				recorderState = RecorderState.LEFT;
			}

			// sende Markierung an alle Ausgangskanäle
			if (leftNeighbor.equals(rightNeighbor)) {
				forwarder.sendSnapshotMarker(leftNeighbor, marker);
			} else {
				forwarder.sendSnapshotMarker(leftNeighbor, marker);
				forwarder.sendSnapshotMarker(rightNeighbor, marker);
			}

		}else{
			// Beende Aufzeichnungsmodus für c
			if (leftNeighbor.equals(rightNeighbor)) {
				recorderState = RecorderState.IDLE;
			}

			if (sender.equals(leftNeighbor)) {
				if (recorderState == RecorderState.BOTH) {
					recorderState = RecorderState.RIGHT;
				} else if (recorderState == RecorderState.LEFT) {
					recorderState = RecorderState.IDLE;
				}
			} else if (sender.equals(rightNeighbor)) {
				if (recorderState == RecorderState.BOTH) {
					recorderState = RecorderState.LEFT;
				} else if (recorderState == RecorderState.RIGHT) {
					recorderState = RecorderState.IDLE;
				}
			}

			if (recorderState == RecorderState.IDLE) {
				System.out.println("Snapshot complete");
				snapshotComplete = true;
			}
		}
	}
}
