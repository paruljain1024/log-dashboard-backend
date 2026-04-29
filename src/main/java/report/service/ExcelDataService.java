//excel parameters
package report.service;

import org.apache.logging.log4j.util.BiConsumer;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Service;
import report.metrics.MetricsResult;
import report.metrics.TypeStats;

import java.util.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.util.function.BiFunction;


@Service
public class ExcelDataService {

    private final MetricsStorageService storage;

    public ExcelDataService(MetricsStorageService storage) {
        this.storage = storage;
    }

    private MetricsResult getResult() {
        MetricsResult r = storage.get();

        if (r == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Process logs first"
            );
        }

        return r;
    }

    // ================= SUMMARY =================

    public Map<String, Object> getSummary() {

        MetricsResult r = getResult();

        long fired = 0;
        long received = 0;
        long success = 0;

        for (TypeStats s : r.typeStatsMap.values()) {

            fired += s.totalRequests.get();
            received += s.requestReceived.get();
            success += s.successCount.get();
        }

        return Map.of(
                "totalRequestsFired", fired,
                "totalResponsesReceived", received,
                "totalSuccess", success
        );
    }

    // ================= TYPE TABLE =================

    public List<Map<String,Object>> getTypeStats() {

        MetricsResult r = getResult();

        List<Map<String,Object>> list = new ArrayList<>();

        for (var e : r.typeStatsMap.entrySet()) {

            TypeStats s = e.getValue();

            list.add(Map.of(
                    "type", e.getKey(),
                    "requestsFired", s.totalRequests.get(),
                    "received", s.requestReceived.get(),
                    "success", s.successCount.get()
            ));
        }

        return list;
    }

    // ================= RESPONSE CODES =================

    public Map<String, Long> getResponseCodes() {
        return getResult().responseCodeMap;
    }

    // ================= PERFORMANCE =================

    public List<Map<String,Object>> getPerformance() {

        MetricsResult r = getResult();

        return List.of(
                metric("VAL", r.avgVAL, r.minVAL, r.peakVAL),
                metric("TOP", r.avgTOP, r.minTOP, r.peakTOP),
                metric("RTT", r.avgResponseTime,
                        r.minResponseTime,
                        r.maxResponseTime),
                metric("PPT", r.avgPPT,
                        r.minPPT,
                        r.peakPPT)
        );
    }

    private Map<String,Object> metric(
            String name,
            double avg,
            long min,
            long max) {

        return Map.of(
                "type", name,
                "avg", avg,
                "min", min,
                "max", max
        );
    }

    // ================= PROCESSING TIME =================

    private static final String[] RANGES = {
            ">0 & <51",
            ">50 & <101",
            ">100 & <301",
            ">300 & <501",
            ">500 & <1001",
            ">1000"
    };

    public List<Map<String,Object>> getProcessingTime() {

        MetricsResult r = getResult();

        return List.of(
                bucket("VAL", r.valBuckets),
                bucket("TOP", r.topBuckets),
                bucket("RTT", r.rttBuckets),
                bucket("PPT", r.pptBuckets)
        );
    }

    private Map<String,Object> bucket(
            String type,
            long[] values) {

        List<Map<String,Object>> ranges =
                new ArrayList<>();

        for (int i = 0; i < values.length; i++) {
            ranges.add(Map.of(
                    "range", RANGES[i],
                    "count", values[i]
            ));
        }

        return Map.of(
                "type", type,
                "ranges", ranges
        );
    }

    // ================= TRANSACTION TIME =================

    public Map<String,Object> getTransactionTime() {

        MetricsResult r = getResult();

        return Map.of(
                "average", r.avgPPT,
                "maximum", r.peakPPT
        );
    }

    // ================= ACTIVE SIZE =================

    public Map<String,Object> getActiveSize() {

        MetricsResult r = getResult();

        return Map.of(
                "min", r.minActiveSize,
                "avg",r.avgActiveSize,
                "max", r.maxActiveSize
        );
    }



    public byte[] generateFullReport() {

        MetricsResult r = storage.get();

        if (r == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Process logs first"
            );
        }

        try (Workbook wb = new XSSFWorkbook()) {

            Sheet sheet = wb.createSheet("Load Test Report");

            // ================= STYLES =================
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle sectionStyle = wb.createCellStyle();
            Font sectionFont = wb.createFont();
            sectionFont.setBold(true);
            sectionFont.setColor(IndexedColors.WHITE.getIndex());
            sectionStyle.setFont(sectionFont);
            sectionStyle.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
            sectionStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            sectionStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle tableHeader = wb.createCellStyle();
            Font thFont = wb.createFont();
            thFont.setBold(true);
            tableHeader.setFont(thFont);
            tableHeader.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            tableHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            tableHeader.setBorderTop(BorderStyle.THIN);
            tableHeader.setBorderBottom(BorderStyle.THIN);
            tableHeader.setBorderLeft(BorderStyle.THIN);
            tableHeader.setBorderRight(BorderStyle.THIN);
            tableHeader.setAlignment(HorizontalAlignment.CENTER);

            CellStyle borderStyle = wb.createCellStyle();
            borderStyle.setBorderTop(BorderStyle.THIN);
            borderStyle.setBorderBottom(BorderStyle.THIN);
            borderStyle.setBorderLeft(BorderStyle.THIN);
            borderStyle.setBorderRight(BorderStyle.THIN);

            // ================= HELPER =================
            BiFunction<Integer, Integer, Row> getRow = (rIdx, cIdx) -> {
                Row rr = sheet.getRow(rIdx);
                if (rr == null) rr = sheet.createRow(rIdx);
                return rr;
            };

            int row = 0;

            // ================= LEFT SIDE =================
            Row h = getRow.apply(row++, 0);
            createCell(h, 0, "Parameter", headerStyle);
            createCell(h, 1, "Remark", headerStyle);

            long fired = 0, received = 0, success = 0;

            for (TypeStats s : r.typeStatsMap.values()) {
                fired += s.totalRequests.get();
                received += s.requestReceived.get();
                success += s.successCount.get();
            }

            row = addRow(sheet, row, "Nos of requests Fired", fired, borderStyle);
            row = addRow(sheet, row, "Request Received", received, borderStyle);
            row = addRow(sheet, row, "Successful Transactions", success, borderStyle);
            row = addRow(sheet, row, "Failure", received - success, borderStyle);

            row++;

            for (var e : r.typeStatsMap.entrySet()) {

                Row sec = getRow.apply(row++, 0);
                createCell(sec, 0, e.getKey(), sectionStyle);

                TypeStats s = e.getValue();

                row = addRow(sheet, row, "Nos of requests Fired", s.totalRequests.get(), borderStyle);
                row = addRow(sheet, row, "Request Received", s.requestReceived.get(), borderStyle);
                row = addRow(sheet, row, "Successful Transactions", s.successCount.get(), borderStyle);

                row++;
            }

            // ================= RIGHT SIDE =================
            int col = 5;

            // TITLE
            Row title = getRow.apply(0, col);
            createCell(title, col, "OCI Load Test Report", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, col, col + 5));

            // SUMMARY
            int rRow = 2;

            rRow = addRight(sheet, rRow, col, "Total Recharge Request fired", fired, borderStyle);
            rRow = addRight(sheet, rRow, col, "Total Recharge Response Received", received, borderStyle);
            rRow = addRight(sheet, rRow, col, "Total Recharge Response FAIL", received - success, borderStyle);
            rRow = addRight(sheet, rRow, col, "Total Recharge Response Success", success, borderStyle);

            // PERFORMANCE
            int pRow = rRow + 2;;

            Row pHeader = getRow.apply(pRow++, col);
            createCell(pHeader, col, "Request Type", tableHeader);
            createCell(pHeader, col + 1, "Average", tableHeader);
            createCell(pHeader, col + 2, "Min", tableHeader);
            createCell(pHeader, col + 3, "Max", tableHeader);

            addPerf(sheet, pRow++, col, "VAL", r.avgVAL, r.minVAL, r.peakVAL, borderStyle);
            addPerf(sheet, pRow++, col, "TOP", r.avgTOP, r.minTOP, r.peakTOP, borderStyle);
            addPerf(sheet, pRow++, col, "RTT", r.avgResponseTime, r.minResponseTime, r.maxResponseTime, borderStyle);
            addPerf(sheet, pRow++, col, "PPT", r.avgPPT, r.minPPT, r.peakPPT, borderStyle);

            // BUCKET
            int bRow = pRow + 2;

            Row bTitle = getRow.apply(bRow++, col);
            createCell(bTitle, col, "Request Processing Time in ms", sectionStyle);
            sheet.addMergedRegion(new CellRangeAddress(22, 22, col, col + 6));

            Row range = getRow.apply(bRow++, col);

            String[] ranges = {
                    ">0 & <51", ">50 & <101", ">100 & <301",
                    ">300 & <501", ">500 & <1001", ">1000"
            };

            for (int i = 0; i < ranges.length; i++) {
                createCell(range, col + 1 + i, ranges[i], tableHeader);
            }

            addBucket(sheet, bRow++, col, "VAL", r.valBuckets, borderStyle);
            addBucket(sheet, bRow++, col, "TOP", r.topBuckets, borderStyle);
            addBucket(sheet, bRow++, col, "RTT", r.rttBuckets, borderStyle);
            addBucket(sheet, bRow++, col, "PPT", r.pptBuckets, borderStyle);

// ================= RESPONSE CODE (MOVED LAST) =================
            int rcStart = bRow + 2;

            Row rcTitle = getRow.apply(rcStart++, col);
            createCell(rcTitle, col, "Unique Response Code", sectionStyle);
            sheet.addMergedRegion(new CellRangeAddress(rcStart - 1, rcStart - 1, col, col + 3));

            Row rcHeader = getRow.apply(rcStart++, col);
            createCell(rcHeader, col, "Response Code", tableHeader);
            createCell(rcHeader, col + 1, "Count", tableHeader);

            for (var e : r.responseCodeMap.entrySet()) {
                Row rr = getRow.apply(rcStart++, col);
                createCell(rr, col, e.getKey(), borderStyle);
                createCell(rr, col + 1, e.getValue(), borderStyle);
            }

            // TRANSACTION
            int tRow = row + 2;

            Row tTitle = getRow.apply(tRow++, 0);
            createCell(tTitle, 0, "Transaction Time MS", sectionStyle);

            tRow = addRow(sheet, tRow, "PreTUPS Average", (long) r.avgPPT, borderStyle);
            tRow = addRow(sheet, tRow, "PreTUPS Maximum", r.peakPPT, borderStyle);

            // ✅ FIXED ACTIVE SIZE
            tRow++;

            Row aTitle = getRow.apply(tRow++, 0);
            createCell(aTitle, 0, "Active Size", sectionStyle);

            tRow = addRow(sheet, tRow, "Min Active Size", r.minActiveSize, borderStyle);
            tRow = addRow(sheet, tRow, "Max Active Size", r.maxActiveSize, borderStyle);

            // AUTO SIZE
            for (int i = 0; i < 12; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createCell(Row r, int col, Object value, CellStyle style) {
        Cell c = r.createCell(col);
        c.setCellValue(String.valueOf(value));
        if (style != null) c.setCellStyle(style);
    }

    private int addRow(Sheet sheet, int row, String label, long value, CellStyle style) {
        Row r = sheet.getRow(row);
        if (r == null) r = sheet.createRow(row);

        createCell(r, 0, label, style);
        createCell(r, 1, value, style);

        return row + 1;
    }

    private int addRight(Sheet sheet, int row, int col, String label, long value, CellStyle style) {
        Row r = sheet.getRow(row);
        if (r == null) r = sheet.createRow(row);

        createCell(r, col, label, style);
        createCell(r, col + 1, value, style);

        return row + 1;
    }

    private void addPerf(Sheet sheet, int row, int col, String type, double avg, long min, long max, CellStyle style) {
        Row r = sheet.getRow(row);
        if (r == null) r = sheet.createRow(row);

        createCell(r, col, type, style);
        createCell(r, col + 1, avg, style);
        createCell(r, col + 2, min, style);
        createCell(r, col + 3, max, style);
    }

    private void addBucket(Sheet sheet, int row, int col, String type, long[] values, CellStyle style) {
        Row r = sheet.getRow(row);
        if (r == null) r = sheet.createRow(row);

        createCell(r, col, type, style);

        for (int i = 0; i < values.length; i++) {
            createCell(r, col + 1 + i, values[i], style);
        }
    }

}
