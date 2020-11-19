package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

public class NameResolutionRequest implements Serializable {
    private final String searchedTankId;
    private final String requestId;

    public NameResolutionRequest(String searchedTankId, String requestId) {
        this.searchedTankId = searchedTankId;
        this.requestId = requestId;
    }

    public String getSearchedTankId() {
        return searchedTankId;
    }

    public String getRequestId() {
        return requestId;
    }

}
