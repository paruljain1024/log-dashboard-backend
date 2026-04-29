package report.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import report.metrics.MetricsCalculator;
import report.metrics.MetricsResult;
import report.metrics.ProcessingStats;
import report.parser.LogParserTask;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LogProcessingService {
    private final MetricsStorageService storage;
    private final ProcessingStatusService statusService;
    private final BackupService backupService;

    private final ProcessingStats stats = new ProcessingStats();

    public LogProcessingService(
            MetricsStorageService storage,
            ProcessingStatusService statusService,
            BackupService backupService) {

        this.storage = storage;
        this.statusService = statusService;
        this.backupService = backupService;
    }

    @Async
    public void processLogsAsync() {
        try {
            stats.reset();
            statusService.setRunning();
            storage.clear();
            backupService.clear();

            processLogs();

            // ✅ ONLY mark completed if NOT FAILED
            if (!"FAILED".equals(statusService.getStatus(0).get("status"))) {
                statusService.setCompleted();
            }
        } catch (Exception e) {
            statusService.setFailed(e.getMessage());
            e.printStackTrace();
        }
    }

    public File[] validateAndGetLogFiles(String logDirPath) {
        File dir = new File(logDirPath);

        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException("Invalid log directory path");
        }

        File[] logFiles = dir.listFiles((d, name) -> name.endsWith(".log"));

        if (logFiles == null || logFiles.length == 0) {
            throw new IllegalArgumentException("No log files found");
        }

        return logFiles;
    }

    public void processLogs() throws Exception {
        System.out.println("Processing Started");

        List<File> logFiles = LogFileLocator.findAllLogFiles();
        if (logFiles.isEmpty()) {
            System.out.println("No logs found");

            statusService.setFailed("No log files found");  // 🔥 ADD THIS

            return;  // 🔥 VERY IMPORTANT
        }

        MetricsCalculator globalCalculator = new MetricsCalculator();
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        AtomicInteger processedFiles = new AtomicInteger(0);
        int totalFiles = logFiles.size();

        for (File file : logFiles) {
            executor.submit(() -> {
                MetricsCalculator localCalculator = new MetricsCalculator();

                try {
                    new LogParserTask(file, localCalculator, stats).run();
                    synchronized (globalCalculator) {
                        globalCalculator.merge(localCalculator);
                    }
                } finally {
                    int done = processedFiles.incrementAndGet();
                    int percent = (int) ((done * 100.0) / totalFiles);
                    statusService.updateProgress(percent);
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.HOURS);

        System.out.println("Finalizing Metrics...");

        MetricsResult result = globalCalculator.getResult();
        storage.store(result);
        backupService.save(result, 0);

        System.out.println("Metrics Calculated");
        //statusService.setCompleted();
    }

    public long getProcessingSpeed() {
        return stats.getSpeed();
    }
}
