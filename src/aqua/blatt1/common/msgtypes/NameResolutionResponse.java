package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class NameResolutionResponse implements Serializable {
    private final InetSocketAddress requestedTankAdress;
    private final String requestId;
    private final InetSocketAddress sender;

    public NameResolutionResponse(InetSocketAddress requestedTankId, String requestId, InetSocketAddress sender) {
        this.requestedTankAdress = requestedTankId;
        this.requestId = requestId;
        this.sender = sender;
    }

    public InetSocketAddress getRequestedTankAdress() {
        return requestedTankAdress;
    }

    public String getRequestId() {
        return requestId;
    }

    public InetSocketAddress getSender() {
        return sender;
    }
}

