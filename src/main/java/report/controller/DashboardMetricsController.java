package report.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import report.metrics.MetricsResult;
import report.metrics.SecondMetricStats;
import report.service.MetricsStorageService;
import report.service.BackupService;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TreeMap;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardMetricsController {

    private final MetricsStorageService storage;
    private final BackupService backupService;

    public DashboardMetricsController(
            MetricsStorageService storage,
            BackupService backupService) {

        this.storage = storage;
        this.backupService = backupService;
    }

    private MetricsResult getResult() {

        if (!storage.hasData())
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Run /process/run first"
            );

        return storage.get();
    }

    /* ===============================
        CONVERTER (LONG → STRING)
    =============================== */

    private <T> Map<String, T> convert(Map<Long, T> source) {

        Map<String, T> result = new TreeMap<>();

        for (var e : source.entrySet()) {

            LocalDateTime dt =
                    LocalDateTime.ofEpochSecond(
                            e.getKey(), 0, ZoneOffset.UTC);

            String key = dt.toLocalDate() + " " + dt.toLocalTime();

            result.put(key, e.getValue());
        }

        return result;
    }

    /* ===============================
        BASIC METRICS
    =============================== */

    @GetMapping("/request-in")
    public Map<String, Integer> requestIn() {
        return convert(getResult().requestInPerSec);
    }

    @GetMapping("/request-out")
    public Map<String, Integer> requestOut() {
        return convert(getResult().requestOutPerSec);
    }

    @GetMapping("/active-size")
    public Map<String, Long> activeSize() {
        return convert(getResult().activeSizePerSec);
    }

    @GetMapping("/success")
    public Map<String, Integer> success() {
        return convert(getResult().successPerSec);
    }

    @GetMapping("/failure")
    public Map<String, Integer> failure() {
        return convert(getResult().failurePerSec);
    }

    /* ===============================
        PERFORMANCE METRICS
    =============================== */

    @GetMapping("/val")
    public Map<String, SecondMetricStats> val() {
        return convert(getResult().valPerSec);
    }

    @GetMapping("/top")
    public Map<String, SecondMetricStats> top() {
        return convert(getResult().topPerSec);
    }

    @GetMapping("/ppt")
    public Map<String, SecondMetricStats> ppt() {
        return convert(getResult().pptPerSec);
    }

    @GetMapping("/rtt")
    public Map<String, SecondMetricStats> rtt() {
        return convert(getResult().rttPerSec);
    }

    /* ===============================
        EFFICIENCY
    =============================== */

    @GetMapping("/efficiency")
    public Object efficiency() {

        MetricsResult r = getResult();

        return Map.of(
                "success", convert(r.successPerSec),
                "failure", convert(r.failurePerSec)
        );
    }

    /* ===============================
        BACKUP CONTROL
    =============================== */

    @GetMapping("/clear-backup")
    public String clearBackup() {
        backupService.clear();
        return "Backup cleared successfully";
    }
}