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
    private final LogFileLocator logFileLocator;

    private final ProcessingStats stats = new ProcessingStats();

    public LogProcessingService(
            MetricsStorageService storage,
            ProcessingStatusService statusService,
            BackupService backupService,
            LogFileLocator logFileLocator) {

        this.storage = storage;
        this.statusService = statusService;
        this.backupService = backupService;
        this.logFileLocator = logFileLocator;
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

            if (!"FAILED".equals(
                    statusService.getStatus(
                            0,
                            0,
                            0,
                            0
                    ).get("status")
            )) {
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

        stats.reset();

        List<File> logFiles =
                logFileLocator.findAllLogFiles();

        if (logFiles.isEmpty()) {

            statusService.setFailed("No log files found");

            return;
        }

        long totalBytes = logFiles.stream()
                .mapToLong(File::length)
                .sum();

        stats.setTotalBytes(totalBytes);

        MetricsCalculator globalCalculator =
                new MetricsCalculator();

        int threads = Math.max(
                1,
                Runtime.getRuntime().availableProcessors()
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(threads);

        AtomicInteger processedFiles =
                new AtomicInteger(0);

        int totalFiles = logFiles.size();

        System.out.println(
                "TOTAL FILES: " + totalFiles
        );

        System.out.println(
                "TOTAL THREADS: " + threads
        );

        for (File file : logFiles) {

            System.out.println(
                    "SUBMITTING: " + file.getName()
            );

            executor.submit(() -> {

                MetricsCalculator localCalculator =
                        new MetricsCalculator();

                try {

                    System.out.println(
                            "THREAD STARTED: "
                                    + file.getName()
                    );

                    new LogParserTask(
                            file,
                            localCalculator,
                            stats
                    ).run();

                    System.out.println(
                            "PARSING FINISHED: "
                                    + file.getName()
                    );

                    synchronized (globalCalculator) {

                        System.out.println(
                                "MERGING STARTED: "
                                        + file.getName()
                        );

                        globalCalculator.merge(localCalculator);

                        System.out.println(
                                "MERGING COMPLETED: "
                                        + file.getName()
                        );
                    }

                } catch (Exception e) {

                    System.out.println(
                            "ERROR IN FILE: "
                                    + file.getName()
                    );

                    e.printStackTrace();

                } finally {

                    int done =
                            processedFiles.incrementAndGet();

                    System.out.println(
                            "THREAD FINISHED: "
                                    + file.getName()
                                    + " | DONE = "
                                    + done
                                    + "/"
                                    + totalFiles
                    );

                    statusService.updateProgress(
                            stats.getProgressPercent()
                    );
                }
            });
        }

        System.out.println("ALL TASKS SUBMITTED");

        executor.shutdown();

        System.out.println("WAITING FOR THREADS...");

        boolean finished =
                executor.awaitTermination(
                        30,
                        TimeUnit.HOURS
                );

        System.out.println(
                "THREAD WAIT RESULT: " + finished
        );

        System.out.println("ALL THREADS FINISHED");

        MetricsResult result =
                globalCalculator.getResult();

        System.out.println(
                "SAVING PROCESSING RESULT..."
        );

        storage.setProcessingResult(result);

        System.out.println(
                "PROCESSING RESULT SAVED"
        );

        System.out.println(
                "PROMOTION STARTED"
        );

        storage.promoteProcessingToActive();

        System.out.println(
                "PROMOTION COMPLETED"
        );
    }

    public double getProcessingSpeed() {
        return stats.getMBPerSecond();
    }

    public double getProcessedGB() {
        return stats.getProcessedGB();
    }

    public double getTotalGB() {
        return stats.getTotalGB();
    }

    public long getEtaSeconds() {
        return stats.getRemainingSeconds();
    }
}
