package report.model;

public class RequestStats {
    public long fired;
    public long received;
    public long success;

    public void addFired() { fired++; }
    public void addReceived() { received++; }
    public void addSuccess() { success++; }
}

