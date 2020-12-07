package aqua.blatt1.broker;

import java.util.ArrayList;
import java.util.List;

/*
 * This class is not thread-safe and hence must be used in a thread-safe way, e.g. thread confined or 
 * externally synchronized. 
 */

public class ClientCollection<T> {
	// Frage: In Ordnung, dass hier public gemacht zu haben?
	private class Client {
		final String id;
		final T client;
		private long registrationTime;

		Client(String id, T client, long registrationTime) {
			this.id = id;
			this.client = client;
			this.registrationTime = registrationTime;
		}
	}

	private final List<Client> clients;

	public ClientCollection() {
		clients = new ArrayList<Client>();
	}

	public ClientCollection<T> add(String id, T client, long registrationTime) {
		clients.add(new Client(id, client,registrationTime));
		return this;
	}

	public ClientCollection<T> remove(int index) {
		clients.remove(index);
		return this;
	}

	public int indexOf(String id) {
		for (int i = 0; i < clients.size(); i++)
			if (clients.get(i).id.equals(id))
				return i;
		return -1;
	}

	public int indexOf(T client) {
		for (int i = 0; i < clients.size(); i++)
			if (clients.get(i).client.equals(client))
				return i;
		return -1;
	}

	public T getClient(int index) {
		return clients.get(index).client;
	}

	public int size() {
		return clients.size();
	}

	public T getLeftNeighorOf(int index) {
		return index == 0 ? clients.get(clients.size() - 1).client : clients.get(index - 1).client;
	}

	public T getRightNeighorOf(int index) {
		return index < clients.size() - 1 ? clients.get(index + 1).client : clients.get(0).client;
	}

	public long getRegistrationTime(int index) {
		return clients.get(index).registrationTime;
	}

	public String getId(int index) {
		return clients.get(index).id;
	}

	public void setRegistrationTime(int index, long newRegistrationTime) {
		clients.get(index).registrationTime = newRegistrationTime;
	}



}
