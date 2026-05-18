package report.parser;

import report.metrics.MetricsCalculator;
import report.metrics.ProcessingStats;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
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
    private final long startOffset;
    private final long endOffset;

    public LogParserTask(File logFile,
                         MetricsCalculator calculator,
                         ProcessingStats stats) {
        this(logFile, calculator, stats, 0L, logFile.length());
    }

    public LogParserTask(File logFile,
                         MetricsCalculator calculator,
                         ProcessingStats stats,
                         long startOffset,
                         long endOffset) {
        this.logFile = logFile;
        this.calculator = calculator;
        this.stats = stats;
        this.startOffset = Math.max(0L, startOffset);
        this.endOffset = Math.max(this.startOffset, endOffset);
    }

    @Override
    public void run() {
        try (FileChannel channel = FileChannel.open(
                logFile.toPath(),
                StandardOpenOption.READ
        )) {
            long alignedStart = resolveAlignedStart(channel);
            if (alignedStart >= endOffset) {
                return;
            }

            channel.position(alignedStart);
            readChunk(channel, alignedStart);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse " + logFile.getAbsolutePath(), e);
        }
    }

    private long resolveAlignedStart(FileChannel channel) throws Exception {
        if (startOffset <= 0L) {
            return 0L;
        }

        long scanPosition = startOffset - 1L;
        channel.position(scanPosition);

        ByteBuffer scanBuffer = ByteBuffer.allocate(8192);

        while (true) {
            scanBuffer.clear();
            int read = channel.read(scanBuffer);

            if (read < 0) {
                return channel.size();
            }

            scanBuffer.flip();

            for (int i = 0; i < read; i++) {
                if (scanBuffer.get(i) == '\n') {
                    return scanPosition + i + 1L;
                }
            }

            scanPosition += read;
        }
    }

    private void readChunk(FileChannel channel, long alignedStart) throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 1024);
        ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream(4096);

        long filePointer = alignedStart;
        long lineStart = alignedStart;

        while (true) {
            byteBuffer.clear();
            int read = channel.read(byteBuffer);

            if (read < 0) {
                break;
            }

            if (read == 0) {
                continue;
            }

            byteBuffer.flip();

            int segmentStart = 0;

            for (int i = 0; i < read; i++) {
                if (byteBuffer.get(i) != '\n') {
                    continue;
                }

                int segmentLength = i - segmentStart;
                if (segmentLength > 0) {
                    lineBuffer.write(byteBuffer.array(), segmentStart, segmentLength);
                }

                filePointer += (i - segmentStart) + 1L;

                if (lineStart < endOffset) {
                    processLine(decodeLine(lineBuffer));
                    stats.addProcessedBytes(filePointer - lineStart);
                }

                lineBuffer.reset();
                lineStart = filePointer;
                segmentStart = i + 1;

                if (lineStart >= endOffset) {
                    return;
                }
            }

            int remaining = read - segmentStart;
            if (remaining > 0) {
                lineBuffer.write(byteBuffer.array(), segmentStart, remaining);
                filePointer += remaining;
            }
        }

        if (lineBuffer.size() > 0 && lineStart < endOffset) {
            processLine(decodeLine(lineBuffer));
            stats.addProcessedBytes(filePointer - lineStart);
        }
    }

    private String decodeLine(ByteArrayOutputStream lineBuffer) {
        byte[] bytes = lineBuffer.toByteArray();
        int length = bytes.length;

        if (length > 0 && bytes[length - 1] == '\r') {
            length--;
        }

        return new String(bytes, 0, length, StandardCharsets.UTF_8);
    }

    private void processLine(String line) {
        stats.increment();

        int reqInIndex = line.indexOf(REQ_IN_MARKER);
        int reqOutIndex = line.indexOf(REQ_OUT_MARKER);
        int dailyLogIndex = line.indexOf(DAILY_LOG_MARKER);

        int activeSizeIndex = -1;
        if (reqInIndex < 0 && reqOutIndex < 0 && dailyLogIndex < 0) {
            activeSizeIndex = indexOfIgnoreCase(line, ACTIVE_SIZE_MARKER);
            if (activeSizeIndex < 0) {
                return;
            }
        }

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

        if (reqInIndex >= 0) {
            reqIn = true;
            type = normalizeType(extractTagValue(line, TYPE_OPEN, TYPE_CLOSE));
        } else if (reqOutIndex >= 0) {
            reqOut = true;
            type = normalizeType(extractTagValue(line, TYPE_OPEN, TYPE_CLOSE));
            txnStatus = extractTagValue(line, STATUS_OPEN, STATUS_CLOSE);
        }

        if (dailyLogIndex >= 0) {
            boolean isRC = line.contains("[STV:RC]");

            if (isRC) {
                val = extractMetric(line, "[VAL:");
                top = extractMetric(line, "[TOP:");
            }

            ppt = extractMetric(line, "[PPT:");
            rtt = extractMetric(line, "[RTT:");
        }

        if (activeSizeIndex >= 0) {
            activeSize = extractActiveSize(line, activeSizeIndex);
        }

        if (reqIn || reqOut || activeSize != null ||
                val != null || top != null || ppt != null || rtt != null) {

            calculator.updateMetrics(
                    time, reqIn, reqOut, type, txnStatus,
                    activeSize, val, top, ppt, rtt
            );
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

    private Long extractActiveSize(String line, int markerIndex) {
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
        if (raw == null) {
            return null;
        }
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

            if (value < 0 || value > 100000) {
                return null;
            }

            return (long) value;

        } catch (Exception e) {

            return null;
        }
    }
}
