//storing calculated metric
package report.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class MetricsResult implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public Map<String, TypeStats> typeStatsMap = new HashMap<>();
    public Map<String, Long> responseCodeMap = new HashMap<>();

    public double avgVAL, avgTOP, avgPPT, avgResponseTime;
    public long minVAL = Long.MAX_VALUE, peakVAL = 0;
    public long minTOP = Long.MAX_VALUE, peakTOP = 0;
    public long minPPT = Long.MAX_VALUE, peakPPT = 0;
    public long minResponseTime = Long.MAX_VALUE, maxResponseTime = Long.MIN_VALUE;

    public long[] valBuckets = new long[6];
    public long[] topBuckets = new long[6];
    public long[] pptBuckets = new long[6];
    public long[] rttBuckets = new long[6];

    public long minActiveSize = Long.MAX_VALUE;
    public long maxActiveSize = Long.MIN_VALUE;

    public AtomicLong totalActiveSamples = new AtomicLong();
    public AtomicLong totalActiveSize = new AtomicLong();

    public double avgActiveSize = 0;

    public Map<Long, Integer> requestInPerSec = new HashMap<>();
    public Map<Long, Integer> requestOutPerSec = new HashMap<>();
    public Map<Long, Integer> successPerSec = new HashMap<>();
    public Map<Long, Integer> failurePerSec = new HashMap<>();
    public Map<Long, Integer> refusedPerSec = new HashMap<>();

    public Map<Long, SecondMetricStats> activeSizePerSec = new HashMap<>();

    public Map<Long, SecondMetricStats> rttPerSec = new HashMap<>();
    public Map<Long, SecondMetricStats> valPerSec = new HashMap<>();
    public Map<Long, SecondMetricStats> topPerSec = new HashMap<>();
    public Map<Long, SecondMetricStats> pptPerSec = new HashMap<>();

    public Map<String, Map<Long, Long>> typeRequestInPerSec = new HashMap<>();
    public Map<String, Map<Long, Long>> typeRequestOutPerSec = new HashMap<>();
}
