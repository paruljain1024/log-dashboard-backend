//status tracking
package report.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ProcessingStatusService {

    private volatile String status = "NOT_STARTED";
    private volatile int progress = 0;
    private volatile String errorMessage;

    private volatile long startTime;
    private volatile long endTime;

    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    public boolean tryStart() {
        return isProcessing.compareAndSet(false, true);
    }

    public void setRunning() {
        status = "PROCESSING";
        progress = 0;
        errorMessage = null;
        startTime = System.currentTimeMillis();
        endTime = 0;
    }

    public void setCompleted() {
        status = "COMPLETED";
        progress = 100;
        endTime = System.currentTimeMillis();
        isProcessing.set(false);
    }

    public void setFailed(String reason) {
        status = "FAILED";
        errorMessage = reason;
        isProcessing.set(false);
    }

    public void updateProgress(int p) {
        progress = p;
    }

    public boolean isProcessing() {
        return isProcessing.get();
    }

    public Map<String, Object> getStatus(
            double speed,
            double processedGB,
            double totalGB,
            long etaSeconds
    ) {

        Map<String, Object> map = new HashMap<>();

        map.put("status", status);

        map.put("progress", progress);

        map.put("speed", speed);

        map.put("processedGB", processedGB);

        map.put("totalGB", totalGB);

        map.put("etaSeconds", etaSeconds);

        map.put("startTime", startTime);

        map.put("endTime", endTime);

        if (errorMessage != null) {
            map.put("message", errorMessage);
        }

        return map;
    }
}