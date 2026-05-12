// processing api
// after loading this the backend start processing logs

package report.controller;

import org.springframework.web.bind.annotation.*;
import report.service.LogFileLocator;
import report.service.LogProcessingService;
import report.service.ProcessingStatusService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/process")
@CrossOrigin("*")
public class ProcessingController {

    private final LogFileLocator logFileLocator;
    private final LogProcessingService service;
    private final ProcessingStatusService statusService;

    public ProcessingController(
            LogProcessingService service,
            ProcessingStatusService statusService,
            LogFileLocator logFileLocator
    ) {

        this.service = service;
        this.statusService = statusService;
        this.logFileLocator = logFileLocator;
    }

    @GetMapping("/run")
    public Map<String, Object> startProcessing() {

        System.out.println("PROCESS STARTED");

        Map<String, Object> res = new HashMap<>();

        if (statusService.isProcessing()) {

            res.put("started", false);
            res.put("message", "Processing already running");

            return res;
        }

        var logFiles = logFileLocator.findAllLogFiles();

        if (logFiles.isEmpty()) {

            res.put("started", false);
            res.put("message", "No log files found");

            return res;
        }

        service.processLogsAsync();

        res.put("started", true);
        res.put("message", "Processing started");

        return res;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {

        double speed = service.getProcessingSpeed();

        return statusService.getStatus(speed);
    }

    @GetMapping("/check")
    public Map<String, Object> checkLogs() {

        Map<String, Object> res = new HashMap<>();

        var logFiles = logFileLocator.findAllLogFiles();

        res.put("hasLogs", !logFiles.isEmpty());
        res.put("count", logFiles.size());

        return res;
    }
}