package report.metrics;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

public class TypeStats implements Serializable {

    private static final long serialVersionUID = 1L;

    public AtomicLong totalRequests = new AtomicLong();
    public AtomicLong requestReceived = new AtomicLong();
    public AtomicLong successCount = new AtomicLong();
    public AtomicLong refusedCount  = new AtomicLong();

}
