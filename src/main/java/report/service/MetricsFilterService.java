package report.service;

import org.springframework.stereotype.Service;
import report.dto.FilteredChartPoint;
import report.metrics.SecondMetricStats;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MetricsFilterService {

    /* ===============================
        MAIN NUMERIC FILTER (LONG BASED)
    =============================== */

    public Map<String, Number> filterMetric(
            Map<Long, ? extends Number> source,
            String date,
            String from,
            String to,
            int intervalSeconds
    ) {

        LocalTime fromTime =
                from == null ? LocalTime.MIN : parseFlexibleTime(from);

        LocalTime toTime =
                to == null ? LocalTime.MAX : parseFlexibleTime(to);

        Map<String, Double> sumMap = new TreeMap<>();
        Map<String, Integer> countMap = new TreeMap<>();

        for (Map.Entry<Long, ? extends Number> e : source.entrySet()) {

            long ts = e.getKey();

            LocalDateTime dt =
                    LocalDateTime.ofEpochSecond(ts, 0, ZoneOffset.UTC);

            String entryDate = dt.toLocalDate().toString();
            LocalTime t = dt.toLocalTime();

            // DATE FILTER
            if (date != null && !date.equals(entryDate))
                continue;

            // TIME FILTER
            if (t.isBefore(fromTime) || t.isAfter(toTime))
                continue;

            // BUCKET
            String bucket = getBucket(ts, intervalSeconds);

            sumMap.put(bucket,
                    sumMap.getOrDefault(bucket, 0.0)
                            + e.getValue().doubleValue());

            countMap.put(bucket,
                    countMap.getOrDefault(bucket, 0) + 1);
        }

        Map<String, Number> result = new TreeMap<>();

        for (String key : sumMap.keySet()) {
            result.put(key, sumMap.get(key) / countMap.get(key));
        }

        return result;
    }

    /* ===============================
        SIMPLE AVG CONVERSION
    =============================== */
    public Map<String, Number> filterSecondMetric(
            Map<Long, SecondMetricStats> source,
            String date,
            String from,
            String to,
            int intervalSeconds
    ) {

        Map<Long, Number> converted = new TreeMap<>();

        for (var e : source.entrySet()) {

            if (e.getValue().getCount() == 0)
                continue;

            converted.put(
                    e.getKey(),
                    e.getValue().getAvg()
            );
        }

        return filterMetric(
                converted,
                date,
                from,
                to,
                intervalSeconds
        );
    }

    /* ===============================
        PERFORMANCE METRICS (MIN/AVG/MAX)
    =============================== */
    public Map<String, FilteredChartPoint> filterSecondMetricStats(
            Map<Long, SecondMetricStats> source,
            String date,
            String from,
            String to,
            int intervalSeconds
    ) {

        LocalTime fromTime =
                parseFlexibleTime(from) == null
                        ? LocalTime.MIN
                        : parseFlexibleTime(from);

        LocalTime toTime =
                parseFlexibleTime(to) == null
                        ? LocalTime.MAX
                        : parseFlexibleTime(to);

        Map<String, StatsBucket> buckets = new TreeMap<>();

        for (var e : source.entrySet()) {

            long ts = e.getKey();

            LocalDateTime dt =
                    LocalDateTime.ofEpochSecond(ts, 0, ZoneOffset.UTC);

            String entryDate = dt.toLocalDate().toString();
            LocalTime time = dt.toLocalTime();

            // DATE FILTER
            if (date != null && !date.equals(entryDate))
                continue;

            // TIME FILTER
            if (time.isBefore(fromTime) || time.isAfter(toTime))
                continue;

            var s = e.getValue();

            if (s.getCount() == 0)
                continue;

            String bucketKey = getBucket(ts, intervalSeconds);

            buckets.computeIfAbsent(bucketKey, key -> new StatsBucket())
                    .add(s);
        }

        Map<String, FilteredChartPoint> result = new TreeMap<>();

        for (var entry : buckets.entrySet()) {
            StatsBucket bucket = entry.getValue();

            result.put(entry.getKey(),
                    new FilteredChartPoint(
                            bucket.min,
                            bucket.getAvg(),
                            bucket.max
                    ));
        }

        return result;
    }

    /* ===============================
        TYPEWISE FILTER (PARALLEL)
    =============================== */
    public Map<String, Map<String, Number>> filterTypewise(
            Map<String, Map<Long, Long>> source,
            String date,
            String from,
            String to,
            int intervalSeconds
    ) {

        Map<String, Map<String, Number>> result =
                new ConcurrentHashMap<>();

        source.entrySet()
                .parallelStream()
                .forEach(entry -> {

                    String type = entry.getKey();
                    Map<Long, Long> data = entry.getValue();

                    Map<String, Number> filtered =
                            filterMetric(data, date, from, to, intervalSeconds);

                    result.put(type, filtered);
                });

        return result;
    }

    /* ===============================
        BUCKET LOGIC (LONG BASED)
    =============================== */
    private String getBucket(long ts, int interval) {

        LocalDateTime dt =
                LocalDateTime.ofEpochSecond(ts, 0, ZoneOffset.UTC);

        int bucketSecond =
                (dt.toLocalTime().toSecondOfDay() / interval) * interval;

        LocalTime bucketTime =
                LocalTime.ofSecondOfDay(bucketSecond);

        return dt.toLocalDate() + " " + bucketTime.toString();
    }

    /* ===============================
        FORMAT TIMESTAMP FOR OUTPUT
    =============================== */
    private String formatTimestamp(LocalDateTime dt) {
        return dt.toLocalDate() + " " + dt.toLocalTime().toString();
    }

    /* ===============================
        FLEXIBLE TIME PARSER
    =============================== */
    private LocalTime parseFlexibleTime(String time) {

        if (time == null || time.isBlank())
            return null;

        if (time.length() == 5) {
            time = time + ":00";
        }

        if (time.length() == 7) {
            time = "0" + time;
        }

        return LocalTime.parse(time);
    }

    private static class StatsBucket {
        private double sum = 0;
        private long count = 0;
        private long min = Long.MAX_VALUE;
        private long max = Long.MIN_VALUE;

        private void add(SecondMetricStats stats) {
            long sampleCount = stats.getCount();

            if (sampleCount <= 0) {
                return;
            }

            sum += stats.getAvg() * sampleCount;
            count += sampleCount;
            min = Math.min(min, stats.getMin());
            max = Math.max(max, stats.getMax());
        }

        private double getAvg() {
            return count == 0 ? 0 : sum / count;
        }
    }
}
