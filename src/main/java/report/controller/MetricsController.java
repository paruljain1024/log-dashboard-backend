//all metrics
package report.controller;

import org.springframework.web.bind.annotation.*;
import report.metrics.MetricsResult;
import report.service.MetricsStorageService;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final MetricsStorageService storage;

    public MetricsController(MetricsStorageService storage) {
        this.storage = storage;
    }

    @GetMapping("/all")
    public MetricsResult getAllMetrics() {

        if (!storage.hasData()) {
            throw new RuntimeException("No data processed yet");
        }

        return storage.get();
    }
}