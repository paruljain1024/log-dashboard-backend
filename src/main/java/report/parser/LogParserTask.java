package report.parser;

import report.metrics.MetricsCalculator;
import report.metrics.ProcessingStats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class LogParserTask implements Runnable {

    private static final DateTimeFormatter PRETUPS_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss a", Locale.ENGLISH);
    private static final String REQ_IN_MARKER = "[ReqIn]";
    private static final String REQ_OUT_MARKER = "[ReqOut:";
    private static final String DAILY_LOG_MARKER = "ChannelRequestDailyLog";
    private static final String TYPE_OPEN = "<TYPE>";
    private static final String TYPE_CLOSE = "</TYPE>";
    private static final String STATUS_OPEN = "<TXNSTATUS>";
    private static final String STATUS_CLOSE = "</TXNSTATUS>";
    private static final String ACTIVE_SIZE_MARKER = "active size";

    private final ProcessingStats stats;
    private final File logFile;
    private final MetricsCalculator calculator;

    public LogParserTask(File logFile,
                         MetricsCalculator calculator,
                         ProcessingStats stats) {

        this.logFile = logFile;
        this.calculator = calculator;
        this.stats = stats;
    }

    @Override
    public void run() {
        try (BufferedReader br = new BufferedReader(new FileReader(logFile), 1024 * 1024)) {
            String line;
            while ((line = br.readLine()) != null) {
                processLine(line);
                stats.addProcessedBytes(
                        line.length()
                );
            }

            System.out.println("Parsing completed: " + logFile.getName());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse " + logFile.getAbsolutePath(), e);
        }
    }

    private void processLine(String line) {
        stats.increment();

        LocalDateTime time = parseTime(line);
        if (time == null) {
            return;
        }

        boolean reqIn = false;
        boolean reqOut = false;
        String type = null;
        String txnStatus = null;
        Long activeSize = null;
        Long val = null;
        Long top = null;
        Long ppt = null;
        Long rtt = null;

        if (line.contains(REQ_IN_MARKER)) {
            reqIn = true;
            String rawType = extractTagValue(line, TYPE_OPEN, TYPE_CLOSE);
            if (rawType != null) {
                type = normalizeType(rawType);
            }
        } else if (line.contains(REQ_OUT_MARKER)) {
            reqOut = true;
            String rawType = extractTagValue(line, TYPE_OPEN, TYPE_CLOSE);
            if (rawType != null) {
                type = normalizeType(rawType);
            }
            txnStatus = extractTagValue(line, STATUS_OPEN, STATUS_CLOSE);
        }

        if (line.contains(DAILY_LOG_MARKER)) {

            // 🔥 ONLY RC transactions for VAL/TOP
            boolean isRC = line.contains("[STV:RC]");

            if (isRC) {

                val = extractMetric(line, "[VAL:");

                top = extractMetric(line, "[TOP:");
            }

            // 🔥 RTT/PPT from all logs
            ppt = extractMetric(line, "[PPT:");

            rtt = extractMetric(line, "[RTT:");
        }

        if (containsIgnoreCase(line, ACTIVE_SIZE_MARKER)) {
            activeSize = extractActiveSize(line);
        }

        if (reqIn || reqOut || activeSize != null ||
                val != null || top != null || ppt != null || rtt != null) {

            calculator.updateMetrics(
                    time, reqIn, reqOut, type, txnStatus,
                    activeSize, val, top, ppt, rtt
            );

            if (type != null) {
                calculator.updateTypeTimeSeries(time, type, reqIn, reqOut);
            }
        }
    }

    private LocalDateTime parseTime(String line) {
        try {
            int firstSpace = line.indexOf(' ');
            if (firstSpace <= 0) {
                return null;
            }

            if (line.indexOf('T') < firstSpace) {
                return OffsetDateTime.parse(line.substring(0, firstSpace)).toLocalDateTime();
            }

            int secondSpace = line.indexOf(' ', firstSpace + 1);
            if (secondSpace < 0) {
                return null;
            }

            int thirdSpace = line.indexOf(' ', secondSpace + 1);
            if (thirdSpace < 0) {
                return null;
            }

            int fourthSpace = line.indexOf(' ', thirdSpace + 1);
            if (fourthSpace < 0) {
                return null;
            }

            return LocalDateTime.parse(line.substring(0, fourthSpace), PRETUPS_TIME_FORMAT);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractTagValue(String line, String openTag, String closeTag) {
        int start = line.indexOf(openTag);
        if (start < 0) {
            return null;
        }

        start += openTag.length();
        int end = line.indexOf(closeTag, start);
        if (end < 0) {
            return null;
        }

        return line.substring(start, end);
    }

    private Long extractActiveSize(String line) {
        int markerIndex = indexOfIgnoreCase(line, ACTIVE_SIZE_MARKER);
        if (markerIndex < 0) {
            return null;
        }

        int start = markerIndex + ACTIVE_SIZE_MARKER.length();
        while (start < line.length()) {
            char ch = line.charAt(start);
            if (Character.isDigit(ch)) {
                break;
            }
            if (ch != ' ' && ch != ':') {
                return null;
            }
            start++;
        }

        int end = start;
        while (end < line.length() && Character.isDigit(line.charAt(end))) {
            end++;
        }

        if (start == end) {
            return null;
        }

        try {
            return Long.parseLong(line.substring(start, end));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean containsIgnoreCase(String line, String needle) {
        return indexOfIgnoreCase(line, needle) >= 0;
    }

    private int indexOfIgnoreCase(String line, String needle) {
        int max = line.length() - needle.length();
        for (int i = 0; i <= max; i++) {
            if (line.regionMatches(true, i, needle, 0, needle.length())) {
                return i;
            }
        }
        return -1;
    }

    private String normalizeType(String raw) {
        if (raw.endsWith("REQ")) {
            return raw.substring(0, raw.length() - 3);
        }
        if (raw.endsWith("RESP")) {
            return raw.substring(0, raw.length() - 4);
        }
        return raw;
    }

    private Long extractMetric(String line, String key) {

        int start = line.indexOf(key);

        if (start == -1) {
            return null;
        }

        start += key.length();

        int end = start;

        while (end < line.length()) {

            char ch = line.charAt(end);

            if (!Character.isDigit(ch) && ch != '.') {
                break;
            }

            end++;
        }

        try {

            double value =
                    Double.parseDouble(
                            line.substring(start, end)
                    );

            // 🔥 FILTER INVALID HUGE TIMINGS
            if (value < 0 || value > 100000) {
                return null;
            }

            return (long) value;

        } catch (Exception e) {

            return null;
        }
    }
}
