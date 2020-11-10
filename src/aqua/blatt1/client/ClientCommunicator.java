package aqua.blatt1.client;

import java.net.InetSocketAddress;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;

public class ClientCommunicator {
	private final Endpoint endpoint;

	public ClientCommunicator() {
		endpoint = new Endpoint();
	}

	public class ClientForwarder {
		private final InetSocketAddress broker;

		private ClientForwarder() {
			this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
		}

		public void register() {
			endpoint.send(broker, new RegisterRequest());
		}

		public void deregister(String id) {
			endpoint.send(broker, new DeregisterRequest(id));
		}

		public void sendToken(InetSocketAddress receiver,Token token){
			endpoint.send(receiver, token);
		}

		public void sendSnapshotMarker (InetSocketAddress receiver, SnapshotMarker marker){
			System.out.println("Sende Snapshotmarker");
			endpoint.send(receiver,marker);
		}

		public void sendSnapshotCollector (InetSocketAddress receiver, SnapshotCollector marker){
			System.out.println("Sende Snapshotcollector");
			endpoint.send(receiver,marker);
		}

		public void handOff(FishModel fish, InetSocketAddress left, InetSocketAddress right) {
			InetSocketAddress receiverAdress;

			if (fish.getDirection() == Direction.LEFT) {
				receiverAdress = left;
			}else{
				receiverAdress = right;
			}
			System.out.print("Ausgabe "+receiverAdress.toString());
			endpoint.send(receiverAdress,new HandoffRequest(fish));
		}
	}

	public class ClientReceiver extends Thread {
		private final TankModel tankModel;

		private ClientReceiver(TankModel tankModel) {
			this.tankModel = tankModel;
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				Message msg = endpoint.blockingReceive();

				if (msg.getPayload() instanceof RegisterResponse)
					tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId());

				if (msg.getPayload() instanceof HandoffRequest)
					tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish());

				if(msg.getPayload() instanceof NeighborUpdate)
					tankModel.updateNeighbors(((NeighborUpdate) msg.getPayload()).getAddressLeft(),((NeighborUpdate) msg.getPayload()).getAddressRight());

				if(msg.getPayload() instanceof Token){
					tankModel.receiveToken((Token) msg.getPayload());
				}

				if(msg.getPayload() instanceof SnapshotMarker){
					System.out.println("Erhalte Snapshotmarker");
					tankModel.receiveSnapshotMarker(msg.getSender(),(SnapshotMarker) msg.getPayload());
				}

				if(msg.getPayload() instanceof SnapshotCollector){
					System.out.println("Erhalte SnapshotCollector");
					tankModel.receiveSnapshotCollector((SnapshotCollector) msg.getPayload());
				}
			}
			System.out.println("Receiver stopped.");
		}
	}

	public ClientForwarder newClientForwarder() {
		return new ClientForwarder();
	}

	public ClientReceiver newClientReceiver(TankModel tankModel) {
		return new ClientReceiver(tankModel);
	}

}
