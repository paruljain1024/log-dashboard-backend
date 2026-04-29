//This stores per-second min/avg/max values for charts.
package report.metrics;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

public class SecondMetricStats implements Serializable {

    private static final long serialVersionUID = 1L;

    private AtomicLong sum = new AtomicLong();
    private AtomicLong count = new AtomicLong();
    private AtomicLong min = new AtomicLong(Long.MAX_VALUE);
    private AtomicLong max = new AtomicLong(Long.MIN_VALUE);

    public void add(long value) {

        sum.addAndGet(value);
        count.incrementAndGet();

        min.updateAndGet(prev -> Math.min(prev, value));
        max.updateAndGet(prev -> Math.max(prev, value));
    }

    public long getCount() {
        return count.get();
    }

    public long getMin() {
        return min.get();
    }

    public long getMax() {
        return max.get();
    }

    public double getAvg() {

        long c = count.get();

        if (c == 0) return 0;

        return (double) sum.get() / c;
    }

    public void merge(SecondMetricStats other) {

        if (other == null) return;

        // ✅ SUM + COUNT
        this.count.addAndGet(other.count.get());
        this.sum.addAndGet(other.sum.get());

        // ✅ MIN
        this.min.updateAndGet(current ->
                Math.min(current, other.min.get())
        );

        // ✅ MAX
        this.max.updateAndGet(current ->
                Math.max(current, other.max.get())
        );
    }


}
