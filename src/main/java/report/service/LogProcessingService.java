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

        // 🔥 block if already processing
        if (!statusService.tryStart()) {
            System.out.println("Already processing - request ignored");
            return;
        }

        try {
            statusService.setRunning();

            processLogs();

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

        ProcessingStats stats = new ProcessingStats(); // ✅ LOCAL now

        List<File> logFiles = LogFileLocator.findAllLogFiles();

        if (logFiles.isEmpty()) {
            statusService.setFailed("No log files found");
            return;
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

        MetricsResult result = globalCalculator.getResult();

        // 🔥 IMPORTANT: DO NOT overwrite active immediately
        storage.setProcessingResult(result);

        // 🔥 SAFE SWAP
        storage.promoteProcessingToActive();
    }

    public long getProcessingSpeed() {
        return stats.getSpeed();
    }
}
