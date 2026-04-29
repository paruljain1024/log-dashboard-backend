package report.metrics;

public class MetricsSummary {
    public int totalRecordsParsed;
    public int totalTransactions;
    public int successCount;
    public int failureCount;

    public double avgTps;
    public double maxTps;

    public double avgResponseTime;
    public long minResponseTime;
    public long maxResponseTime;

    public double avgPpt;
    public long peakPpt;

    public double avgTop;
    public long peakTop;

    public double avgVal;
    public long peakVal;

    public int spikeEvents;
}

