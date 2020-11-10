package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

public class SnapshotCollector implements Serializable {
    public int counter;

    public SnapshotCollector(int localCount) {
        this.counter = localCount;
    }

    public void addToSnapshot(int localCounter) {
        this.counter = this.counter + localCounter;
    }

    public int getCounter() {
        return counter;
    }
}
