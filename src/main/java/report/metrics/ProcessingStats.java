//Real-time processing speed indicator
package report.metrics;

import java.util.concurrent.atomic.AtomicLong;

public class ProcessingStats {

    private final AtomicLong totalLines = new AtomicLong();
    private final AtomicLong linesLastSecond = new AtomicLong();

    public void increment() {
        totalLines.incrementAndGet();
        linesLastSecond.incrementAndGet();
    }

    public long getTotalLines() {
        return totalLines.get();
    }

    public long getSpeed() {
        return linesLastSecond.getAndSet(0); // reset every second
    }

    public void reset() {
        totalLines.set(0);
        linesLastSecond.set(0);
    }
}
