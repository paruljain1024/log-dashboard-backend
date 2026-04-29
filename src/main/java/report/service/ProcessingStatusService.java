//status tracking
package report.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ProcessingStatusService {
    private volatile String errorMessage;

    private volatile String status = "NOT_STARTED";
    private volatile int progress = 0;

    public void setRunning() {
        status = "PROCESSING";
        progress = 0;
        errorMessage = null;
    }

    public void setCompleted() {
        status = "COMPLETED";
        progress = 100;
        errorMessage = null;
    }

    public void updateProgress(int p) {
        progress = p;
    }

    public Map<String,Object> getStatus(long speed) {

        Map<String,Object> map = new HashMap<>();

        map.put("status", status);
        map.put("progress", progress);
        map.put("speed", speed);
        if (errorMessage != null) {
            map.put("message", errorMessage);
        }

        return map;
    }
    public void setFailed(String reason) {
        status = "FAILED";
        errorMessage = reason;
    }
}
