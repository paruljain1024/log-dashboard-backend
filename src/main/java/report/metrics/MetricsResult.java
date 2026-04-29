//storing calculated metric
package report.metrics;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class MetricsResult implements java.io.Serializable {


    private static final long serialVersionUID = 1L;

    // LEFT SIDE (Dynamic TYPE table)
    public Map<String, TypeStats> typeStatsMap = new ConcurrentHashMap<>();

    // RIGHT SIDE (Response codes)
    public Map<String, Long> responseCodeMap = new ConcurrentHashMap<>();

    // Timing metrics
    public double avgVAL, avgTOP, avgPPT, avgResponseTime;
    public long minVAL = Long.MAX_VALUE, peakVAL = 0;
    public long minTOP = Long.MAX_VALUE, peakTOP = 0;
    public long minPPT = Long.MAX_VALUE, peakPPT = 0;
    public long minResponseTime = Long.MAX_VALUE, maxResponseTime = Long.MIN_VALUE;

    // TD bucket counts
    public long[] valBuckets = new long[6];
    public long[] topBuckets = new long[6];
    public long[] pptBuckets = new long[6];
    public long[] rttBuckets = new long[6];

    // Active Size
    public long minActiveSize = Long.MAX_VALUE;
    public long maxActiveSize = Long.MIN_VALUE;

    public AtomicLong totalActiveSamples = new AtomicLong();
    public AtomicLong totalActiveSize = new AtomicLong();

    public double avgActiveSize = 0;

    // ===== Time-series data for graphical report =====
    public Map<Long, Integer> requestInPerSec = new ConcurrentHashMap<>();
    public Map<Long, Integer> requestOutPerSec = new ConcurrentHashMap<>();
    public Map<Long, Integer> successPerSec = new ConcurrentHashMap<>();
    public Map<Long, Integer> failurePerSec = new ConcurrentHashMap<>();
    public Map<Long, Integer> refusedPerSec = new ConcurrentHashMap<>();


    public Map<Long, Long> activeSizePerSec = new ConcurrentHashMap<>();

    public Map<Long, SecondMetricStats> rttPerSec = new ConcurrentHashMap<>();
    public Map<Long, SecondMetricStats> valPerSec = new ConcurrentHashMap<>();
    public Map<Long, SecondMetricStats> topPerSec = new ConcurrentHashMap<>();
    public Map<Long, SecondMetricStats> pptPerSec = new ConcurrentHashMap<>();


    public Map<String, Map<Long, Long>> typeRequestInPerSec = new ConcurrentHashMap<>();
    public Map<String, Map<Long, Long>> typeRequestOutPerSec = new ConcurrentHashMap<>();

}
