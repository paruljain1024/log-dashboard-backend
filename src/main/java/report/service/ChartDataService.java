package report.service;

import org.springframework.stereotype.Service;
import report.dto.ChartPoint;
import report.metrics.MetricsResult;
import report.metrics.SecondMetricStats;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class ChartDataService {

    private final MetricsStorageService storage;

    public ChartDataService(MetricsStorageService storage) {
        this.storage = storage;
    }

    /* ===============================
        GET NORMAL METRICS
    =============================== */

    public List<ChartPoint> getMetric(String metric) {

        MetricsResult r = storage.get();

        switch (metric.toLowerCase()) {

            case "request-in":
                return convert(r.requestInPerSec);

            case "request-out":
                return convert(r.requestOutPerSec);

            case "active-size":
                return convert(r.activeSizePerSec);

            case "val":
                return convertStats(r.valPerSec);

            case "ppt":
                return convertStats(r.pptPerSec);

            case "rtt":
                return convertStats(r.rttPerSec);

            case "top":
                return convertStats(r.topPerSec);

            default:
                throw new RuntimeException("Unknown metric");
        }
    }

    /* ===============================
        NUMERIC CONVERSION
    =============================== */

    private List<ChartPoint> convert(
            Map<Long, ? extends Number> source) {

        List<ChartPoint> result = new ArrayList<>();

        for (var e : new TreeMap<>(source).entrySet()) {

            LocalDateTime dt = toDateTime(e.getKey());

            result.add(
                    new ChartPoint(
                            dt.toLocalDate().toString(),
                            dt.toLocalTime().toString(),
                            e.getValue()
                    )
            );
        }

        return result;
    }

    /* ===============================
        STATS CONVERSION
    =============================== */

    private List<ChartPoint> convertStats(
            Map<Long, SecondMetricStats> source) {

        List<ChartPoint> result = new ArrayList<>();

        for (var e : new TreeMap<>(source).entrySet()) {

            var s = e.getValue();

            if (s.getCount() == 0) continue;

            LocalDateTime dt = toDateTime(e.getKey());

            result.add(
                    new ChartPoint(
                            dt.toLocalDate().toString(),
                            dt.toLocalTime().toString(),
                            s.getMin(),
                            s.getAvg(),
                            s.getMax()
                    )
            );
        }

        return result;
    }

    /* ===============================
        CUSTOM FILTER (USED LESS NOW)
    =============================== */

    public List<ChartPoint> getCustomMetric(
            String metric,
            String date,
            String from,
            String to,
            int interval
    ) {

        MetricsResult r = storage.get();

        switch (metric.toLowerCase()) {

            case "request-in":
                return filterNumeric(r.requestInPerSec, date, from, to, interval);

            case "request-out":
                return filterNumeric(r.requestOutPerSec, date, from, to, interval);

            case "active-size":
                return filterNumeric(r.activeSizePerSec, date, from, to, interval);

            case "val":
                return filterStats(r.valPerSec, date, from, to, interval);

            case "ppt":
                return filterStats(r.pptPerSec, date, from, to, interval);

            case "rtt":
                return filterStats(r.rttPerSec, date, from, to, interval);

            case "top":
                return filterStats(r.topPerSec, date, from, to, interval);

            default:
                throw new RuntimeException("Unknown metric");
        }
    }

    /* ===============================
        FILTER NUMERIC
    =============================== */

    private List<ChartPoint> filterNumeric(
            Map<Long, ? extends Number> source,
            String date,
            String from,
            String to,
            int interval) {

        List<ChartPoint> result = new ArrayList<>();

        int counter = 0;
        double sum = 0;

        for (var e : new TreeMap<>(source).entrySet()) {

            LocalDateTime dt = toDateTime(e.getKey());

            if (!match(dt, date, from, to)) continue;

            sum += e.getValue().doubleValue();
            counter++;

            if (counter == interval) {

                result.add(new ChartPoint(
                        dt.toLocalDate().toString(),
                        dt.toLocalTime().toString(),
                        sum / counter
                ));

                counter = 0;
                sum = 0;
            }
        }

        return result;
    }

    /* ===============================
        FILTER STATS
    =============================== */

    private List<ChartPoint> filterStats(
            Map<Long, SecondMetricStats> source,
            String date,
            String from,
            String to,
            int interval) {

        List<ChartPoint> result = new ArrayList<>();

        int counter = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        double sum = 0;

        for (var e : new TreeMap<>(source).entrySet()) {

            LocalDateTime dt = toDateTime(e.getKey());
            var s = e.getValue();

            if (!match(dt, date, from, to)) continue;
            if (s.getCount() == 0) continue;

            min = Math.min(min, s.getMin());
            max = Math.max(max, s.getMax());
            sum += s.getAvg();

            counter++;

            if (counter == interval) {

                result.add(new ChartPoint(
                        dt.toLocalDate().toString(),
                        dt.toLocalTime().toString(),
                        min,
                        sum / counter,
                        max
                ));

                counter = 0;
                sum = 0;
                min = Double.MAX_VALUE;
                max = Double.MIN_VALUE;
            }
        }

        return result;
    }

    /* ===============================
        MATCH FILTER
    =============================== */

    private boolean match(
            LocalDateTime dt,
            String date,
            String from,
            String to) {

        String d = dt.toLocalDate().toString();
        String t = dt.toLocalTime().toString();

        if (date != null && !date.equals(d))
            return false;

        if (from != null && t.compareTo(from) < 0)
            return false;

        if (to != null && t.compareTo(to) > 0)
            return false;

        return true;
    }

    /* ===============================
        TYPEWISE RAW DATA
    =============================== */

    public Map<String, Object> getTypewiseTimeSeries() {

        MetricsResult r = storage.get();

        return Map.of(
                "requestIn", r.typeRequestInPerSec,
                "requestOut", r.typeRequestOutPerSec
        );
    }

    /* ===============================
        UTILITY
    =============================== */

    private LocalDateTime toDateTime(long ts) {
        return LocalDateTime.ofEpochSecond(ts, 0, ZoneOffset.UTC);
    }
}