package report.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import report.metrics.MetricsCalculator;
import report.metrics.MetricsResult;
import report.metrics.ProcessingStats;
import report.parser.LogParserTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class LogProcessingService {
    private static final long CHUNK_SIZE_BYTES = 512L * 1024L * 1024L;

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

        CompletionService<ChunkResult> completionService =
                new ExecutorCompletionService<>(executor);

        List<ChunkPlan> chunkPlans = buildChunkPlans(logFiles);
        int totalChunks = chunkPlans.size();

        for (ChunkPlan chunkPlan : chunkPlans) {
            completionService.submit(() -> {
                MetricsCalculator localCalculator =
                        new MetricsCalculator();

                new LogParserTask(
                        chunkPlan.file(),
                        localCalculator,
                        stats,
                        chunkPlan.startOffset(),
                        chunkPlan.endOffset()
                ).run();

                return new ChunkResult(chunkPlan.order(), localCalculator);
            });
        }

        mergeCompletedChunks(
                completionService,
                totalChunks,
                globalCalculator
        );

        executor.shutdown();

        boolean finished =
                executor.awaitTermination(
                        30,
                        TimeUnit.HOURS
                );

        if (!finished) {
            throw new IllegalStateException("Log processing did not finish within the configured timeout");
        }

        MetricsResult result =
                globalCalculator.getResult();

        storage.setProcessingResult(result);
        storage.promoteProcessingToActive();
    }

    private List<ChunkPlan> buildChunkPlans(List<File> logFiles) {
        List<ChunkPlan> plans = new ArrayList<>();
        int order = 0;

        for (File file : logFiles) {
            long fileSize = file.length();

            if (fileSize <= CHUNK_SIZE_BYTES) {
                plans.add(new ChunkPlan(
                        order++,
                        file,
                        0L,
                        fileSize
                ));
                continue;
            }

            for (long start = 0L; start < fileSize; start += CHUNK_SIZE_BYTES) {
                long end = Math.min(fileSize, start + CHUNK_SIZE_BYTES);
                plans.add(new ChunkPlan(
                        order++,
                        file,
                        start,
                        end
                ));
            }
        }

        return plans;
    }

    private void mergeCompletedChunks(
            CompletionService<ChunkResult> completionService,
            int totalChunks,
            MetricsCalculator globalCalculator
    ) throws Exception {
        PriorityQueue<ChunkResult> ready =
                new PriorityQueue<>(Comparator.comparingInt(ChunkResult::order));

        int nextOrder = 0;

        for (int i = 0; i < totalChunks; i++) {
            ChunkResult completed = completionService.take().get();
            ready.add(completed);

            while (!ready.isEmpty() && ready.peek().order() == nextOrder) {
                ChunkResult result = ready.poll();
                globalCalculator.merge(result.calculator());
                nextOrder++;
            }

            statusService.updateProgress(
                    stats.getProgressPercent()
            );
        }
    }

    private record ChunkPlan(
            int order,
            File file,
            long startOffset,
            long endOffset
    ) {
    }

    private record ChunkResult(
            int order,
            MetricsCalculator calculator
    ) {
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
