package report.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import report.metrics.MetricsResult;
import report.service.MetricsFilterService;
import report.service.MetricsStorageService;
import report.service.TypeNameMapper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/filter")
@CrossOrigin(origins = "*")
public class FilterController {

    private static final int MAX_CACHE_SIZE = 100;
    private static final long TTL = 5 * 60 * 1000;

    static class CacheEntry {
        Object value;
        long timestamp;

        CacheEntry(Object value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private final Map<String, CacheEntry> cache =
            Collections.synchronizedMap(
                    new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true) {
                        protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                            return size() > MAX_CACHE_SIZE;
                        }
                    }
            );

    private final MetricsStorageService storage;
    private final MetricsFilterService filterService;
    private final TypeNameMapper typeNameMapper;

    public FilterController(
            MetricsStorageService storage,
            MetricsFilterService filterService,
            TypeNameMapper typeNameMapper) {

        this.storage = storage;
        this.filterService = filterService;
        this.typeNameMapper = typeNameMapper;

        storage.setCacheClearCallback(cache::clear);
    }

    private String buildKey(
            String metric,
            String date,
            String from,
            String to,
            int interval
    ) {
        return metric + "|" +
                (date == null ? "ALL" : date) + "|" +
                (from == null ? "START" : from) + "|" +
                (to == null ? "END" : to) + "|" +
                interval;
    }

    @GetMapping("/custom")
    public Object customMetric(
            @RequestParam String metric,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "1") int interval
    ) {

        String key = buildKey(metric, date, from, to, interval);
        CacheEntry entry = cache.get(key);

        if (entry != null) {
            long age = System.currentTimeMillis() - entry.timestamp;

            if (age < TTL) {
                return entry.value;
            }

            cache.remove(key);
        }

        MetricsResult r = storage.get();
        Object result;

        switch (metric.toLowerCase()) {
            case "request-in":
                result = filterService.filterMetric(
                        r.requestInPerSec, date, from, to, interval);
                break;
            case "request-out":
                result = filterService.filterMetric(
                        r.requestOutPerSec, date, from, to, interval);
                break;
            case "active-size":
                result = filterService.filterSecondMetricStats(
                        r.activeSizePerSec, date, from, to, interval);
                break;
            case "val":
                result = filterService.filterSecondMetricStats(
                        r.valPerSec, date, from, to, interval);
                break;
            case "ppt":
                result = filterService.filterSecondMetricStats(
                        r.pptPerSec, date, from, to, interval);
                break;
            case "rtt":
                result = filterService.filterSecondMetricStats(
                        r.rttPerSec, date, from, to, interval);
                break;
            case "top":
                result = filterService.filterSecondMetricStats(
                        r.topPerSec, date, from, to, interval);
                break;
            case "success":
                result = filterService.filterMetric(
                        r.successPerSec, date, from, to, interval);
                break;
            case "failure":
                result = filterService.filterMetric(
                        r.failurePerSec, date, from, to, interval);
                break;
            case "efficiency":
                result = Map.of(
                        "success",
                        filterService.filterMetric(
                                r.successPerSec, date, from, to, interval),
                        "failure",
                        filterService.filterMetric(
                                r.failurePerSec, date, from, to, interval)
                );
                break;
            case "typewise-timeseries":
                result = Map.of(
                        "requestIn",
                        remapTypewise(filterService.filterTypewise(
                                r.typeRequestInPerSec, date, from, to, interval)),
                        "requestOut",
                        remapTypewise(filterService.filterTypewise(
                                r.typeRequestOutPerSec, date, from, to, interval))
                );
                break;
            default:
                throw new RuntimeException("Unknown metric");
        }

        cache.put(key, new CacheEntry(result));
        return result;
    }

    @GetMapping("/metrics")
    public Object getMetric(
            @RequestParam String metric
    ) {

        MetricsResult result = storage.get();

        switch (metric.toLowerCase()) {
            case "request-in":
                return result.requestInPerSec;
            case "request-out":
                return result.requestOutPerSec;
            case "active-size":
                return result.activeSizePerSec;
            case "val":
                return result.valPerSec;
            case "ppt":
                return result.pptPerSec;
            case "rtt":
                return result.rttPerSec;
            case "top":
                return result.topPerSec;
            case "efficiency":
                return Map.of(
                        "success", result.successPerSec,
                        "failure", result.failurePerSec
                );
            default:
                throw new RuntimeException("Unknown metric: " + metric);
        }
    }

    private Map<String, Map<String, Number>> remapTypewise(
            Map<String, Map<String, Number>> source
    ) {
        Map<String, Map<String, Number>> result = new LinkedHashMap<>();

        source.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> result.put(
                        typeNameMapper.getDisplayName(entry.getKey()),
                        entry.getValue()
                ));

        return result;
    }
}
