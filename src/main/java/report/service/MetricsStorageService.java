//storing latest calculated data in metrics result
package report.service;

import org.springframework.stereotype.Service;
import report.metrics.MetricsResult;

@Service
public class MetricsStorageService {

    private MetricsResult latestResult;

    private Runnable cacheClearCallback;

    public void setCacheClearCallback(Runnable callback) {
        this.cacheClearCallback = callback;
    }


    public void store(MetricsResult result) {
        this.latestResult = result;

        runCacheClearCallback();
    }

    public MetricsResult get() {
        return latestResult;
    }

    public boolean hasData() {
        return latestResult != null;
    }

    public void clear() {
        latestResult = null;
        runCacheClearCallback();
    }

    private void runCacheClearCallback() {
        if (cacheClearCallback != null) {
            cacheClearCallback.run();
        }
    }
}
