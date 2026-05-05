package report.service;

import org.springframework.stereotype.Service;
import report.metrics.MetricsResult;

@Service
public class MetricsStorageService {

    // 🔥 ACTIVE RESULT (used by all users)
    private volatile MetricsResult activeResult;

    // 🔥 PROCESSING RESULT (building in background)
    private volatile MetricsResult processingResult;

    private Runnable cacheClearCallback;

    public void setCacheClearCallback(Runnable callback) {
        this.cacheClearCallback = callback;
    }

    // 🔹 called during processing completion
    public void setProcessingResult(MetricsResult result) {
        this.processingResult = result;
    }

    // 🔹 swap safely after processing completes
    public synchronized void promoteProcessingToActive() {
        if (processingResult != null) {
            this.activeResult = processingResult;
            this.processingResult = null;

            runCacheClearCallback(); // clear filter cache
        }
    }

    // 🔹 always return ACTIVE result
    public MetricsResult get() {
        return activeResult;
    }

    public boolean hasData() {
        return activeResult != null;
    }

    private void runCacheClearCallback() {
        if (cacheClearCallback != null) {
            cacheClearCallback.run();
        }
    }
}