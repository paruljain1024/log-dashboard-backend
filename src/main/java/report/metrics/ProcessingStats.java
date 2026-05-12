package report.metrics;

import java.util.concurrent.atomic.AtomicLong;

public class ProcessingStats {

    // 🔥 Line tracking
    private final AtomicLong totalLines = new AtomicLong();
    private final AtomicLong linesLastSecond = new AtomicLong();

    // 🔥 Byte tracking
    private final AtomicLong processedBytes = new AtomicLong();

    private volatile long totalBytes = 0;

    private volatile long startTime =
            System.currentTimeMillis();

    // =========================================
    // LINE SPEED
    // =========================================

    public void increment() {

        totalLines.incrementAndGet();

        linesLastSecond.incrementAndGet();
    }

    public long getTotalLines() {
        return totalLines.get();
    }

    public long getSpeed() {

        // lines/sec
        return linesLastSecond.getAndSet(0);
    }

    // =========================================
    // BYTE PROGRESS
    // =========================================

    public void addProcessedBytes(long bytes) {
        processedBytes.addAndGet(bytes);
    }

    public long getProcessedBytes() {
        return processedBytes.get();
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public int getProgressPercent() {

        if (totalBytes <= 0) {
            return 0;
        }

        return (int) (
                (processedBytes.get() * 100.0)
                        / totalBytes
        );
    }

    // =========================================
    // MB/sec
    // =========================================

    public double getMBPerSecond() {

        long elapsedSeconds =
                (System.currentTimeMillis() - startTime) / 1000;

        if (elapsedSeconds <= 0) {
            return 0;
        }

        return (
                processedBytes.get()
                        / 1024.0
                        / 1024.0
        ) / elapsedSeconds;
    }

    // =========================================
    // RESET
    // =========================================

    public synchronized void reset() {

        totalLines.set(0);

        linesLastSecond.set(0);

        processedBytes.set(0);

        totalBytes = 0;

        startTime = System.currentTimeMillis();
    }
}