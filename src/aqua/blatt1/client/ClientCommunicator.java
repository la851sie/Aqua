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

		public void sendLocationRequest(InetSocketAddress receiver, LocationRequest locationRequest){
			endpoint.send(receiver,locationRequest);
		}

		public void sendNameResolutionRequest(String searchedTankId, String requestId ){
			endpoint.send(broker, new NameResolutionRequest(searchedTankId, requestId));
		}

		public void sendLocationUpdate(InetSocketAddress receiver, LocationUpdate locationUpdate) {
			endpoint.send(receiver, locationUpdate);
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

				if (msg.getPayload() instanceof RegisterResponse){
					System.out.println("Erhalte RegisterResponse");
					tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId(), ((RegisterResponse) msg.getPayload()).getLeaseDuration());
				}

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

				if(msg.getPayload() instanceof NameResolutionResponse){
					tankModel.handleNameResolutionResponse(((NameResolutionResponse) msg.getPayload()).getRequestedTankAdress(), ((NameResolutionResponse) msg.getPayload()).getRequestId(), ((NameResolutionResponse) msg.getPayload()).getSender());
				}

				if(msg.getPayload() instanceof LocationRequest){
					tankModel.locateFishLocally(((LocationRequest) msg.getPayload()).getFishId());
				}

				if (msg.getPayload() instanceof LocationUpdate) {
					tankModel.handleLocationUpdate(((LocationUpdate) msg.getPayload()).getFishId(), ((LocationUpdate) msg.getPayload()).getInetSocketAddress());
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


