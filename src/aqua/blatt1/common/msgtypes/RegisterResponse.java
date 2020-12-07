package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class RegisterResponse implements Serializable {
	private final String id;
	private int leaseDuration;

	public RegisterResponse(String id, int leaseDuration) {
		this.id = id;
		this.leaseDuration = leaseDuration;
	}

	public String getId() {
		return id;
	}

	public int getLeaseDuration() {
		return leaseDuration;
	}

}
