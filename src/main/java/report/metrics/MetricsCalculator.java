package report.metrics;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetricsCalculator {

    private final MetricsResult r = new MetricsResult();

    private long sumVAL = 0, countVAL = 0;
    private long sumTOP = 0, countTOP = 0;
    private long sumPPT = 0, countPPT = 0;
    private long sumRTT = 0, countRTT = 0;
    private long totalActiveSize = 0, countActiveSize = 0;

    public void updateMetrics(
            LocalDateTime time,
            boolean reqIn,
            boolean reqOut,
            String type,
            String txnStatus,
            Long activeSize,
            Long val,
            Long top,
            Long ppt,
            Long rtt
    ) {

        long secondKey = time.toEpochSecond(java.time.ZoneOffset.UTC);

        if (reqIn && type != null) {
            r.requestInPerSec.merge(secondKey, 1, Integer::sum);
            r.typeStatsMap
                    .computeIfAbsent(type, key -> new TypeStats())
                    .totalRequests
                    .incrementAndGet();
        }

        if (reqOut && type != null) {
            r.requestOutPerSec.merge(secondKey, 1, Integer::sum);

            TypeStats typeStats = r.typeStatsMap
                    .computeIfAbsent(type, key -> new TypeStats());

            typeStats.requestReceived.incrementAndGet();
            r.responseCodeMap.merge(
                    normalizeStatus(txnStatus),
                    1L,
                    Long::sum
            );

            if (isSuccess(txnStatus)) {

                r.successPerSec.merge(secondKey, 1, Integer::sum);
                typeStats.successCount.incrementAndGet();
            } else if (isRefused(txnStatus)) {
                typeStats.refusedCount.incrementAndGet();
            } else {
                r.failurePerSec.merge(secondKey, 1, Integer::sum);
            }
        }

        if (activeSize != null) {
            r.activeSizePerSec.put(secondKey, activeSize);
            r.minActiveSize = Math.min(r.minActiveSize, activeSize);
            r.maxActiveSize = Math.max(r.maxActiveSize, activeSize);
            totalActiveSize += activeSize;
            countActiveSize++;
            r.avgActiveSize = countActiveSize == 0
                    ? 0
                    : (double) totalActiveSize / countActiveSize;
        }

        if (val != null && val > 0) {
            sumVAL += val;
            countVAL++;
            r.avgVAL = (double) sumVAL / countVAL;
            r.minVAL = Math.min(r.minVAL, val);
            r.peakVAL = Math.max(r.peakVAL, val);
            incrementBucket(r.valBuckets, val);
            r.valPerSec
                    .computeIfAbsent(secondKey, k -> new SecondMetricStats())
                    .add(val);
        }

        if (top != null && top > 0) {
            sumTOP += top;
            countTOP++;
            r.avgTOP = (double) sumTOP / countTOP;
            r.minTOP = Math.min(r.minTOP, top);
            r.peakTOP = Math.max(r.peakTOP, top);
            incrementBucket(r.topBuckets, top);
            r.topPerSec
                    .computeIfAbsent(secondKey, k -> new SecondMetricStats())
                    .add(top);
        }

        if (ppt != null && ppt>0) {
            sumPPT += ppt;
            countPPT++;
            r.avgPPT = (double) sumPPT / countPPT;
            r.minPPT = Math.min(r.minPPT, ppt);
            r.peakPPT = Math.max(r.peakPPT, ppt);
            incrementBucket(r.pptBuckets, ppt);
            r.pptPerSec
                    .computeIfAbsent(secondKey, k -> new SecondMetricStats())
                    .add(ppt);
        }

        if (rtt != null && rtt>0) {
            sumRTT += rtt;
            countRTT++;
            r.avgResponseTime = (double) sumRTT / countRTT;
            r.minResponseTime = Math.min(r.minResponseTime, rtt);
            r.maxResponseTime = Math.max(r.maxResponseTime, rtt);
            incrementBucket(r.rttBuckets, rtt);
            r.rttPerSec
                    .computeIfAbsent(secondKey, k -> new SecondMetricStats())
                    .add(rtt);
        }
    }

    public void updateTypeTimeSeries(
            LocalDateTime time,
            String type,
            boolean reqIn,
            boolean reqOut
    ) {

        if (type == null) return;

        long timestamp = time.toEpochSecond(java.time.ZoneOffset.UTC);

        if (reqIn) {
            r.typeRequestInPerSec
                    .computeIfAbsent(type, k -> new ConcurrentHashMap<>())
                    .merge(timestamp, 1L, Long::sum);
        }

        if (reqOut) {
            r.typeRequestOutPerSec
                    .computeIfAbsent(type, k -> new ConcurrentHashMap<>())
                    .merge(timestamp, 1L, Long::sum);
        }
    }

    public MetricsResult getResult() {
        return r;
    }

    /* ===============================
        🔥 MERGE (CRITICAL FIX)
    =============================== */

    public synchronized void merge(MetricsCalculator other) {

        MetricsResult o = other.getResult();

        other.recomputeDerivedFields();

        o.requestInPerSec.forEach(
                (k,v) -> r.requestInPerSec.merge(k,v,Integer::sum));

        o.requestOutPerSec.forEach(
                (k,v) -> r.requestOutPerSec.merge(k,v,Integer::sum));

        o.successPerSec.forEach(
                (k,v) -> r.successPerSec.merge(k,v,Integer::sum));

        o.failurePerSec.forEach(
                (k,v) -> r.failurePerSec.merge(k,v,Integer::sum));

        o.activeSizePerSec.forEach(
                (k,v) -> r.activeSizePerSec.put(k,v));

        mergeStats(r.valPerSec, o.valPerSec);
        mergeStats(r.topPerSec, o.topPerSec);
        mergeStats(r.pptPerSec, o.pptPerSec);
        mergeStats(r.rttPerSec, o.rttPerSec);

        mergeTypewise(r.typeRequestInPerSec, o.typeRequestInPerSec);
        mergeTypewise(r.typeRequestOutPerSec, o.typeRequestOutPerSec);
        mergeTypeStats(o);
        o.responseCodeMap.forEach(
                (k, v) -> r.responseCodeMap.merge(k, v, Long::sum)
        );

        sumVAL += other.sumVAL;
        countVAL += other.countVAL;
        sumTOP += other.sumTOP;
        countTOP += other.countTOP;
        sumPPT += other.sumPPT;
        countPPT += other.countPPT;
        sumRTT += other.sumRTT;
        countRTT += other.countRTT;
        totalActiveSize += other.totalActiveSize;
        countActiveSize += other.countActiveSize;

        mergeBuckets(r.valBuckets, o.valBuckets);
        mergeBuckets(r.topBuckets, o.topBuckets);
        mergeBuckets(r.pptBuckets, o.pptBuckets);
        mergeBuckets(r.rttBuckets, o.rttBuckets);

        if (o.minVAL > 0) {
            r.minVAL = (r.minVAL == Long.MAX_VALUE)
                    ? o.minVAL
                    : Math.min(r.minVAL, o.minVAL);
        }
        r.peakVAL = Math.max(r.peakVAL, o.peakVAL);
        if (o.minTOP > 0) {
            r.minTOP = (r.minTOP == Long.MAX_VALUE)
                    ? o.minTOP
                    : Math.min(r.minTOP, o.minTOP);
        }

        r.peakTOP = Math.max(r.peakTOP, o.peakTOP);
        if (o.minPPT > 0) {
            r.minPPT = (r.minPPT == Long.MAX_VALUE)
                    ? o.minPPT
                    : Math.min(r.minPPT, o.minPPT);
        }
        r.peakPPT = Math.max(r.peakPPT, o.peakPPT);
        if (o.minResponseTime > 0) {
            r.minResponseTime =
                    (r.minResponseTime == Long.MAX_VALUE)
                            ? o.minResponseTime
                            : Math.min(r.minResponseTime,
                            o.minResponseTime);
        }
        r.maxResponseTime = Math.max(r.maxResponseTime, o.maxResponseTime);
        r.minActiveSize = Math.min(r.minActiveSize, o.minActiveSize);
        r.maxActiveSize = Math.max(r.maxActiveSize, o.maxActiveSize);

        recomputeDerivedFields();
    }

    private void mergeStats(Map<Long, SecondMetricStats> base,
                            Map<Long, SecondMetricStats> other) {

        other.forEach((k,v) -> {
            base.computeIfAbsent(k, x -> new SecondMetricStats())
                    .merge(v);
        });
    }

    private void mergeTypewise(
            Map<String, Map<Long, Long>> base,
            Map<String, Map<Long, Long>> other) {

        other.forEach((type, map) -> {

            base.computeIfAbsent(type, t -> new ConcurrentHashMap<>());

            map.forEach((ts, val) -> {
                base.get(type).merge(ts, val, Long::sum);
            });
        });
    }

    private void mergeTypeStats(MetricsResult other) {
        other.typeStatsMap.forEach((type, stats) -> {
            TypeStats merged = r.typeStatsMap.computeIfAbsent(type, key -> new TypeStats());
            merged.totalRequests.addAndGet(stats.totalRequests.get());
            merged.requestReceived.addAndGet(stats.requestReceived.get());
            merged.successCount.addAndGet(stats.successCount.get());
            merged.refusedCount.addAndGet(stats.refusedCount.get());
        });
    }

    private void mergeBuckets(long[] base, long[] other) {
        for (int i = 0; i < base.length; i++) {
            base[i] += other[i];
        }
    }

    private void incrementBucket(long[] buckets, long value) {
        if (value <= 50) {
            buckets[0]++;
        } else if (value <= 100) {
            buckets[1]++;
        } else if (value <= 300) {
            buckets[2]++;
        } else if (value <= 500) {
            buckets[3]++;
        } else if (value <= 1000) {
            buckets[4]++;
        } else {
            buckets[5]++;
        }
    }

    private boolean isSuccess(String txnStatus) {
        return "200".equals(txnStatus) || "SUCCESS".equalsIgnoreCase(txnStatus);
    }

    private boolean isRefused(String txnStatus) {
        return txnStatus != null && txnStatus.regionMatches(true, 0, "REFUSED", 0, "REFUSED".length())
                || txnStatus != null && txnStatus.regionMatches(true,
                Math.max(0, txnStatus.length() - "REFUSED".length()),
                "REFUSED",
                0,
                "REFUSED".length());
    }

    private String normalizeStatus(String txnStatus) {
        if (txnStatus == null || txnStatus.isBlank()) {
            return "UNKNOWN";
        }
        return txnStatus.trim().toUpperCase();
    }

    private void recomputeDerivedFields() {

        r.avgVAL = countVAL == 0 ? 0 : (double) sumVAL / countVAL;

        r.avgTOP = countTOP == 0 ? 0 : (double) sumTOP / countTOP;

        r.avgPPT = countPPT == 0 ? 0 : (double) sumPPT / countPPT;

        r.avgResponseTime =
                countRTT == 0 ? 0 : (double) sumRTT / countRTT;

        r.avgActiveSize =
                countActiveSize == 0
                        ? 0
                        : (double) totalActiveSize / countActiveSize;

        // 🔥 FIX MIN VALUES

        if (countVAL == 0) {
            r.minVAL = 0;
        } else if (r.minVAL == Long.MAX_VALUE) {
            r.minVAL = 0;
        }

        if (countTOP == 0) {
            r.minTOP = 0;
        } else if (r.minTOP == Long.MAX_VALUE) {
            r.minTOP = 0;
        }

        if (countPPT == 0) {
            r.minPPT = 0;
        } else if (r.minPPT == Long.MAX_VALUE) {
            r.minPPT = 0;
        }

        if (countRTT == 0) {
            r.minResponseTime = 0;
            r.maxResponseTime = 0;
        } else {

            if (r.minResponseTime == Long.MAX_VALUE) {
                r.minResponseTime = 0;
            }

            if (r.maxResponseTime == Long.MIN_VALUE) {
                r.maxResponseTime = 0;
            }
        }

        if (countActiveSize == 0) {
            r.minActiveSize = 0;
            r.maxActiveSize = 0;
        }
    }
}
