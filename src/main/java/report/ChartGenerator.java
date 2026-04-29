//not using - now as we are generating charts from the dashboard
/*package report;


import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import java.text.SimpleDateFormat;
import java.util.Date;

import report.metrics.MetricsResult;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import report.metrics.SecondMetricStats;
import report.metrics.TypeStats;

import java.awt.*;
import java.io.File;
import java.util.Map;

public class ChartGenerator {

    /*private static Date globalStartTime = null;
    private static Date globalEndTime = null;

    private static void updateGlobalTimeRange(Date time) {
        if (globalStartTime == null || time.before(globalStartTime)) globalStartTime = time;
        if (globalEndTime == null || time.after(globalEndTime)) globalEndTime = time;
    }
    private static <T> Map<String, T> sortByTimestamp(Map<String, T> map) {

        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(
                        java.util.LinkedHashMap::new,
                        (m, e) -> m.put(e.getKey(), e.getValue()),
                        Map::putAll
                );
    }


    // ================= COMMON TIME CHART STYLE =================
    private static void styleTimeChart(JFreeChart chart, Color lineColor) {
        chart.setBackgroundPaint(Color.WHITE);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(new Color(245, 247, 250));
        plot.setRangeGridlinePaint(Color.GRAY);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, lineColor);
        renderer.setSeriesStroke(0, new BasicStroke(2.5f));
        plot.setRenderer(renderer);

        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss.SSS"));
        axis.setAutoRange(true);

    }

    // ================= BAR CHART STYLE =================
    private static void styleBarChart(JFreeChart chart) {
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(new Color(220, 220, 220));

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(40, 167, 69));
        renderer.setSeriesPaint(1, new Color(220, 53, 69));

        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 18));
    }

    // ================= REQUEST IN =================
    public static File generateRequestInChart(MetricsResult r, String outputDir, String fileName) throws Exception {
        TimeSeries series = new TimeSeries("Request IN");

        Map<String, Integer> perSecond = new java.util.TreeMap<>();

        for (Map.Entry<String, Integer> e : r.requestInPerSec.entrySet()) {
            String sec = toSecond(e.getKey());
            perSecond.merge(sec, e.getValue(), Integer::sum);
        }

        for (Map.Entry<String, Integer> entry : perSecond.entrySet()) {
            Date time = extractTime(entry.getKey());
            series.addOrUpdate(new Millisecond(time), entry.getValue());
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection(series);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Request IN (TPS Trend)", "Time", "Requests per Second",
                dataset, false, true, false);

        styleTimeChart(chart, new Color(0, 102, 204));

        File file = new File(outputDir + "/" + fileName);
        ChartUtils.saveChartAsPNG(file, chart, 900, 350);
        return file;
    }

    // ================= REQUEST OUT =================
    public static File generateRequestOutChart(MetricsResult r, String outputDir, String fileName) throws Exception {
        TimeSeries series = new TimeSeries("Request OUT");

        Map<String,Integer> perSecond =
                aggregateIntPerSecond(r.requestOutPerSec);

        for(Map.Entry<String,Integer> e : perSecond.entrySet()) {
            series.addOrUpdate(
                    new Millisecond(extractTime(e.getKey())),
                    e.getValue()
            );
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection(series);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Request OUT Trend", "Time", "Responses per Second",
                dataset, false, true, false);

        styleTimeChart(chart, new Color(0, 153, 153));

        File file = new File(outputDir + "/" + fileName);
        ChartUtils.saveChartAsPNG(file, chart, 900, 350);
        return file;
    }

    // ================= ACTIVE SIZE =================
    public static File generateActiveSizeChart(MetricsResult r, String outputDir, String fileName) throws Exception {
        TimeSeries series = new TimeSeries("Active Size");

        Map<String,Long> perSecond =
                aggregateActive(r.activeSizePerSec);

        for(Map.Entry<String, Long> e : perSecond.entrySet()){
            series.addOrUpdate(
                    new Millisecond(extractTime(e.getKey())),
                    e.getValue());
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection(series);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Active Size Trend", "Time", "Active Connections",
                dataset, false, true, false);

        styleTimeChart(chart, new Color(255, 140, 0));

        File file = new File(outputDir + "/" + fileName);
        ChartUtils.saveChartAsPNG(file, chart, 900, 350);
        return file;
    }
    // ================= GENERIC AVG CHART (PPT, RTT, VAL, TOP) =================
    public static File generateAvgChart(
            Map<String, Double> sumMap,
            Map<String, Integer> countMap,
            String title,
            String yLabel,
            String fileName,
            String outputDir) throws Exception {

        TimeSeries series = new TimeSeries(title);

        for (String ts : sortByTimestamp(sumMap).keySet()) {
            Date time = extractTime(ts);
            //updateGlobalTimeRange(time);

            double avg = sumMap.get(ts) / countMap.get(ts);
            series.addOrUpdate(new Millisecond(time), avg);
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection(series);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                title,
                "Time",
                yLabel,
                dataset,
                false, true, false
        );

        styleTimeChart(chart, new Color(153, 0, 153));

        File file = new File(outputDir + "/" + fileName);
        ChartUtils.saveChartAsPNG(file, chart, 900, 350);
        return file;
    }


    // ================= SUCCESS vs FAILURE =================
    public static File generateSuccessFailureChart(MetricsResult r, String outputDir, String fileName) throws Exception {
        long success = 0;
        long total = 0;

        for (TypeStats stats : r.typeStatsMap.values()) {
            success += stats.successCount;
            total += stats.requestReceived;
        }

        long failure = total - success;

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(success, "Success", "Transactions");
        dataset.addValue(failure, "Failure", "Transactions");

        JFreeChart chart = ChartFactory.createBarChart(
                "Success vs Failure Transactions", "Category", "Count",
                dataset, PlotOrientation.VERTICAL, true, true, false);

        styleBarChart(chart);

        File file = new File(outputDir + "/" + fileName);
        ChartUtils.saveChartAsPNG(file, chart, 900, 500);
        return file;
    }

    // ================= EFFICIENCY TREND =================
    public static File generateEfficiencyTrendChart(MetricsResult r, String outputDir, String fileName) throws Exception {
        TimeSeries successSeries = new TimeSeries("Success");
        TimeSeries failureSeries = new TimeSeries("Failure");

        Map<String,Integer> success =
                aggregateIntPerSecond(r.successPerSec);

        Map<String,Integer> failure =
                aggregateIntPerSecond(r.failurePerSec);

        for(Map.Entry<String, Integer> e : success.entrySet())
            successSeries.addOrUpdate(
                    new Millisecond(extractTime(e.getKey())),
                    e.getValue());

        for(Map.Entry<String, Integer> e : failure.entrySet())
            failureSeries.addOrUpdate(
                    new Millisecond(extractTime(e.getKey())),
                    e.getValue());

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(successSeries);
        dataset.addSeries(failureSeries);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Efficiency Trend (Success vs Failure)", "Time", "Transactions",
                dataset, true, true, false);

        styleTimeChart(chart, new Color(40, 167, 69));

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesPaint(1, new Color(220, 53, 69));

        File file = new File(outputDir + "/" + fileName);
        ChartUtils.saveChartAsPNG(file, chart, 900, 350);
        return file;
    }

    // ================= TIME PARSER =================
    private static Date extractTime(String timestamp) throws Exception {
        if (timestamp.contains(" ")) timestamp = timestamp.split(" ")[1];
        if (timestamp.contains("T")) timestamp = timestamp.split("T")[1];

        String[] parts = timestamp.split(":");
        String hh = parts[0];
        String mm = parts.length > 1 ? parts[1] : "00";
        String ss = parts.length > 2 ? parts[2].split("\\.")[0] : "00";

        if (ss.length() == 1) ss = "0" + ss;

        return new SimpleDateFormat("HH:mm:ss").parse(hh + ":" + mm + ":" + ss);
    }
    /*private static Date extractTime(String timestamp) throws Exception {

        if (timestamp == null || timestamp.trim().isEmpty()) {
            throw new IllegalArgumentException("Timestamp empty");
        }

        timestamp = timestamp.trim();

        /* Remove date part if present
        if (timestamp.contains(" ")) {
            timestamp = timestamp.substring(timestamp.indexOf(" ") + 1);
        }
        if (timestamp.contains("T")) {
            timestamp = timestamp.substring(timestamp.indexOf("T") + 1);
        }*/
        // Case 1: ISO format (2025-05-02T06:48:32.111+00:00)
       /* if (timestamp.contains("T")) {

            if (timestamp.contains("+")) {
                timestamp = timestamp.substring(0, timestamp.indexOf("+"));
            }

            timestamp = timestamp.replace("T", " ");

            SimpleDateFormat isoFormat =
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

            return isoFormat.parse(timestamp);
        }
        // Case 2: 12-hour format with AM/PM
        if (timestamp.contains("AM") || timestamp.contains("PM")) {

            SimpleDateFormat twelveHourFormat =
                    new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a");

            return twelveHourFormat.parse(timestamp);
        }

        // Case 3: already 24-hour without millis
        SimpleDateFormat simpleFormat =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        return simpleFormat.parse(timestamp);

        // Now we expect time part only
      String[] parts = timestamp.split(":");

        String hh = "00";
        String mm = "00";
        String ss = "00";

        if (parts.length >= 1) hh = parts[0];
        if (parts.length >= 2) mm = parts[1];
        if (parts.length >= 3) ss = parts[2];

        // Remove milliseconds if present (e.g., 45.123)
        /*if (ss.contains(".")) {
            ss = ss.substring(0, ss.indexOf("."));
        }

        // Fix single-digit seconds like "5" → "05"
        if (ss.length() == 1) {
            ss = "0" + ss;
        }

        String normalizedTime = hh + ":" + mm + ":" + ss;

        return new SimpleDateFormat("HH:mm:ss").parse(normalizedTime);
        if (timestamp.length() > 12) {
            timestamp = timestamp.substring(0, 12); // HH:mm:ss.SSS
        }

        return new SimpleDateFormat("HH:mm:ss.SSS").parse(timestamp);*/
        /*timestamp = timestamp.replace("T", " ");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return sdf.parse(timestamp);
   /* private static Date extractTime(String timestamp) throws Exception {

        if (timestamp == null || timestamp.trim().isEmpty())
            throw new IllegalArgumentException("Timestamp empty");

        timestamp = timestamp.trim();

        // Remove timezone if present (+00:00)
        if (timestamp.contains("+"))
            timestamp = timestamp.substring(0, timestamp.indexOf("+"));

        String[] formats = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        };

        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.ENGLISH);
                return sdf.parse(timestamp);
            } catch (Exception ignored) {}
        }

        throw new java.text.ParseException("Unsupported date format: " + timestamp, 0);
    }
    private static String toSecond(String ts) {
        // yyyy-MM-dd HH:mm:ss.SSS → yyyy-MM-dd HH:mm:ss
        return ts.substring(0, 19);
    }
    private static Map<String,Integer> aggregateIntPerSecond(
            Map<String,Integer> source) {

        Map<String,Integer> result = new java.util.TreeMap<>();

        for(Map.Entry<String,Integer> e : source.entrySet()) {
            String sec = e.getKey().substring(0,19);
            result.merge(sec, e.getValue(), Integer::sum);
        }

        return result;
    }
    private static Map<String,Long> aggregateActive(
            Map<String,Long> source){

        Map<String,Long> result = new java.util.TreeMap<>();

        for(Map.Entry<String, Long> e : source.entrySet()){
            String sec = e.getKey().substring(0,19);
            result.put(sec, e.getValue()); // overwrite = latest
        }

        return result;
    }
    public static File generateMinAvgMaxChart(
            Map<String, SecondMetricStats> statsMap,
            String title,
            String yLabel,
            String outputDir,
            String fileName) throws Exception {

        TimeSeries minSeries = new TimeSeries("Min");
        TimeSeries avgSeries = new TimeSeries("Avg");
        TimeSeries maxSeries = new TimeSeries("Max");

        for (String ts : statsMap.keySet()) {

            Date time = extractTime(ts);
            SecondMetricStats s = statsMap.get(ts);

            if (s.count == 0) continue;

            minSeries.addOrUpdate(new org.jfree.data.time.Second(time), s.min);
            avgSeries.addOrUpdate(new org.jfree.data.time.Second(time), s.avg());
            maxSeries.addOrUpdate(new org.jfree.data.time.Second(time), s.max);
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(minSeries);
        dataset.addSeries(avgSeries);
        dataset.addSeries(maxSeries);

        JFreeChart chart =
                ChartFactory.createTimeSeriesChart(
                        title,
                        "Time",
                        yLabel,
                        dataset,
                        true,
                        true,
                        false);
        // ===== WHITE BACKGROUND =====
        chart.setBackgroundPaint(Color.WHITE);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);

        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        XYLineAndShapeRenderer renderer =
                new XYLineAndShapeRenderer(true, false);

        renderer.setSeriesPaint(0, new Color(0, 150, 0));   // MIN - green
        renderer.setSeriesPaint(1, new Color(0, 102, 204)); // AVG - blue
        renderer.setSeriesPaint(2, new Color(220, 53, 69)); // MAX - red

        renderer.setSeriesStroke(0, new BasicStroke(2f));
        renderer.setSeriesStroke(1, new BasicStroke(2.5f));
        renderer.setSeriesStroke(2, new BasicStroke(2f));

        plot.setRenderer(renderer);

        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(
                new SimpleDateFormat("HH:mm:ss"));

        File file = new File(outputDir + "/" + fileName);
        ChartUtils.saveChartAsPNG(file, chart, 900, 350);

        return file;
    }

    // ======================================================
// MASTER METHOD → GENERATE ALL CHARTS
// ======================================================
    public static void generateAllCharts(
            MetricsResult r,
            String outputDir) throws Exception {

        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        System.out.println("📊 Generating Charts...");

        generateRequestInChart(
                r, outputDir, "request_in.png");

        generateRequestOutChart(
                r, outputDir, "request_out.png");

        generateActiveSizeChart(
                r, outputDir, "active_size.png");

        generateEfficiencyTrendChart(
                r, outputDir, "efficiency.png");

        generateMinAvgMaxChart(
                r.pptPerSec,
                "PPT Trend",
                "PPT (ms)",
                outputDir,
                "ppt.png");

        generateMinAvgMaxChart(
                r.topPerSec,
                "TOP Trend",
                "TOP (ms)",
                outputDir,
                "top.png");

        generateMinAvgMaxChart(
                r.valPerSec,
                "VAL Trend",
                "VAL (ms)",
                outputDir,
                "val.png");

        generateMinAvgMaxChart(
                r.rttPerSec,
                "RTT Trend",
                "RTT (ms)",
                outputDir,
                "rtt.png");

        System.out.println("✅ All charts generated");
    }


}*/