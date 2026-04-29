//not in use
/*package report;

import report.metrics.MetricsResult;
import report.metrics.TypeStats;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import report.service.TypeNameMapper;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ExcelReportGenerator {

    public void generateExcelReport(MetricsResult result,
                                    Path outputPath,
                                    TypeNameMapper mapper) {

        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            XSSFSheet sheet = wb.createSheet("Excel Report");

            sheet.setColumnWidth(0, 9500);
            sheet.setColumnWidth(1, 3500);
            sheet.setColumnWidth(2, 6500);
            sheet.setColumnWidth(3, 2500);
            for (int c = 4; c <= 13; c++) sheet.setColumnWidth(c, 6000);

            CellStyle titleStyle = styleTitle(wb);
            CellStyle blueBarStyle = styleBlueBar(wb);
            CellStyle sectionGreyStyle = styleSectionGrey(wb);
            CellStyle headerGreyStyle = styleHeaderGrey(wb);
            CellStyle normalStyle = styleNormal(wb);
            CellStyle numberStyle = styleNumber(wb);

            for (int i = 0; i < 100; i++) sheet.createRow(i).setHeightInPoints(20);

            buildLeftSection(sheet, result, mapper, sectionGreyStyle, headerGreyStyle, normalStyle, numberStyle);
            buildRightSection(sheet, result, titleStyle, blueBarStyle, headerGreyStyle, normalStyle, numberStyle);

            if (outputPath.getParent() != null) {
                Files.createDirectories(outputPath.getParent());
            }
            try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
                wb.write(fos);
            }

        } catch (Exception e) {
            throw new RuntimeException("Excel generation failed", e);
        }
    }

    // ================= LEFT SIDE =================

    private void buildLeftSection(Sheet sheet,
                                  MetricsResult result,
                                  TypeNameMapper mapper,
                                  CellStyle sectionGrey,
                                  CellStyle headerGrey,
                                  CellStyle normal,
                                  CellStyle numberStyle) {

        // ===== CALCULATE OVERALL TOTALS =====
        long totalFired = 0;
        long totalReceived = 0;
        long totalSuccess = 0;

        for (TypeStats stats : result.typeStatsMap.values()) {
            totalFired += stats.totalRequests;
            totalReceived += stats.requestReceived;
            totalSuccess += stats.successCount;
        }


        setText(sheet, 1, 0, "Parameter", headerGrey);
        setText(sheet, 1, 2, "Remark", headerGrey);

        int row = 2;

        // ===== WRITE TOTAL SUMMARY =====
        setText(sheet, row, 0, "Total Requests Fired", normal);
        setText(sheet, row++, 2, String.valueOf(totalFired), numberStyle);

        setText(sheet, row, 0, "Total Responses Received", normal);
        setText(sheet, row++, 2, String.valueOf(totalReceived), numberStyle);

        setText(sheet, row, 0, "Total Successful Transactions", normal);
        setText(sheet, row++, 2, String.valueOf(totalSuccess), numberStyle);

        // Leave one blank row before type-wise breakdown
        row++;

        for (String type : result.typeStatsMap.keySet()) {

            TypeStats stats = result.typeStatsMap.get(type);
            String display = mapper.getDisplayName(type);

            setText(sheet, row++, 0, display, sectionGrey);

            setText(sheet, row, 0, "Nos of requests Fired", normal);
            setText(sheet, row++, 2, String.valueOf(stats.totalRequests), numberStyle);

            setText(sheet, row, 0, "Request Received", normal);
            setText(sheet, row++, 2, String.valueOf(stats.requestReceived), numberStyle);

            setText(sheet, row, 0, "Successful Transactions", normal);
            setText(sheet, row++, 2, String.valueOf(stats.successCount), numberStyle);
        }

        row++;
        setText(sheet, row++, 0, "CPU Details - App Server", sectionGrey);
        setText(sheet, row++, 0, "Utilization", normal);
        setText(sheet, row++, 0, "Idle %age", normal);
        setText(sheet, row++, 0, "Memory usage (User)", normal);

        row++;
        setText(sheet, row++, 0, "Transaction Time MS", sectionGrey);

        setText(sheet, row, 0, "PreTUPS Average", normal);
        setText(sheet, row++, 2, String.format("%.3f", result.avgPPT), numberStyle);

        setText(sheet, row, 0, "PreTUPS Maximum", normal);
        setText(sheet, row++, 2, String.valueOf(result.peakPPT), numberStyle);

        row++;
        setText(sheet, row++, 0, "Active Size", sectionGrey);

        setText(sheet, row, 0, "Min Active Size", normal);
        setText(sheet, row++, 2, String.valueOf(result.minActiveSize), numberStyle);

        setText(sheet, row, 0, "Max Active Size", normal);
        setText(sheet, row++, 2, String.valueOf(result.maxActiveSize), numberStyle);
    }

    // ================= RIGHT SIDE =================

    private void buildRightSection(Sheet sheet,
                                   MetricsResult result,
                                   CellStyle titleStyle,
                                   CellStyle blueBarStyle,
                                   CellStyle headerGrey,
                                   CellStyle normal,
                                   CellStyle numberStyle) {

        merge(sheet, 1, 1, 4, 13);
        setText(sheet, 1, 4, "Load Test", titleStyle);

        long fired = 0;
        long received = 0;
        long success = 0;
        long refused=0;

        for (Map.Entry<String, TypeStats> entry : result.typeStatsMap.entrySet()) {
            String type = entry.getKey().toUpperCase();

            // any recharge-like request types
            if (type.contains("RC") || type.contains("RECHARGE") || type.contains("EXRCTRF")) {
                TypeStats s = entry.getValue();
                fired += s.totalRequests;
                received += s.requestReceived;
                success += s.successCount;
                refused += s.refusedCount;
            }
        }

        long fail = received - success - refused;


        setText(sheet, 3, 4, "Total Recharge Request fired", normal);
        setText(sheet, 3, 5, String.valueOf(fired), numberStyle);

        setText(sheet, 4, 4, "Total Recharge Response Received", normal);
        setText(sheet, 4, 5, String.valueOf(received), numberStyle);

        setText(sheet, 5, 4, "Total Recharge Response FAIL", normal);
        setText(sheet, 5, 5, String.valueOf(fail), numberStyle);

        setText(sheet, 6, 4, "Total Recharge Response Refused", normal);
        setText(sheet, 6, 5, String.valueOf(refused), numberStyle);

        setText(sheet, 7, 4, "Total Recharge Response Success", normal);
        setText(sheet, 7, 5, String.valueOf(success), numberStyle);

        merge(sheet, 9, 9, 4, 13);
        setText(sheet, 9, 4, "Unique Response Code:", blueBarStyle);

        setText(sheet, 10, 4, "Response Code", headerGrey);
        setText(sheet, 10, 5, "Count", headerGrey);

        int rcRow = 11;
        for (Map.Entry<String, Long> e : result.responseCodeMap.entrySet()) {
            setText(sheet, rcRow, 4, e.getKey(), normal);
            setText(sheet, rcRow++, 5, String.valueOf(e.getValue()), numberStyle);
        }

        setText(sheet, 16, 4, "Request Type", headerGrey);
        setText(sheet, 16, 5, "Average", headerGrey);
        setText(sheet, 16, 6, "Min", headerGrey);
        setText(sheet, 16, 7, "Max", headerGrey);

        writeMetricRow(sheet, 17, "VAL", result.avgVAL, result.minVAL, result.peakVAL, normal, numberStyle);
        writeMetricRow(sheet, 18, "TOP", result.avgTOP, result.minTOP, result.peakTOP, normal, numberStyle);
        writeMetricRow(sheet, 19, "RTT", result.avgResponseTime, result.minResponseTime, result.maxResponseTime, normal, numberStyle);
        writeMetricRow(sheet, 20, "PPT", result.avgPPT, result.minPPT, result.peakPPT, normal, numberStyle);

        merge(sheet, 22, 22, 4, 13);
        setText(sheet, 22, 4, "Request Processing Time in ms", blueBarStyle);

        String[] headers = {">0 & <51", ">50 & <101", ">100 & <301", ">300 & <501", ">500 & <1001", ">1000"};
        setText(sheet, 23, 4, "TD Count", headerGrey);
        for (int i = 0; i < headers.length; i++)
            setText(sheet, 23, 5 + i, headers[i], headerGrey);

        writeTDRow(sheet, 24, "VAL", result.valBuckets, normal, numberStyle);
        writeTDRow(sheet, 25, "TOP", result.topBuckets, normal, numberStyle);
        writeTDRow(sheet, 26, "RTT", result.rttBuckets, normal, numberStyle);
        writeTDRow(sheet, 27, "PPT", result.pptBuckets, normal, numberStyle);
    }

    // ================= STYLES =================

    private CellStyle styleTitle(Workbook wb) {
        CellStyle s = baseStyle(wb, true, IndexedColors.LIGHT_CORNFLOWER_BLUE);
        s.setAlignment(HorizontalAlignment.CENTER);
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 12);
        s.setFont(f);
        return s;
    }

    private CellStyle styleBlueBar(Workbook wb) {
        CellStyle s = baseStyle(wb, true, IndexedColors.PALE_BLUE);
        s.setAlignment(HorizontalAlignment.LEFT);
        return s;
    }

    private CellStyle styleSectionGrey(Workbook wb) {
        CellStyle s = baseStyle(wb, true, IndexedColors.GREY_40_PERCENT);
        s.setAlignment(HorizontalAlignment.LEFT);
        return s;
    }

    private CellStyle styleHeaderGrey(Workbook wb) {
        CellStyle s = baseStyle(wb, true, IndexedColors.GREY_25_PERCENT);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private CellStyle styleNormal(Workbook wb) {
        CellStyle s = baseStyle(wb, false, null);
        s.setAlignment(HorizontalAlignment.LEFT);
        return s;
    }

    private CellStyle styleNumber(Workbook wb) {
        CellStyle s = baseStyle(wb, false, null);
        s.setAlignment(HorizontalAlignment.RIGHT);
        return s;
    }

    private CellStyle baseStyle(Workbook wb, boolean bold, IndexedColors color) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(bold);
        s.setFont(f);
        s.setVerticalAlignment(VerticalAlignment.CENTER);

        if (color != null) {
            s.setFillForegroundColor(color.getIndex());
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }

        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    // ================= HELPERS =================

    private void writeMetricRow(Sheet sheet, int row, String name,
                                double avg, long min, long max,
                                CellStyle normal, CellStyle number) {
        setText(sheet, row, 4, name, normal);
        setText(sheet, row, 5, String.format("%.3f", avg), number);
        setText(sheet, row, 6, String.valueOf(min), number);
        setText(sheet, row, 7, String.valueOf(max), number);
    }

    private void writeTDRow(Sheet sheet, int row, String name,
                            long[] buckets, CellStyle normal, CellStyle number) {
        setText(sheet, row, 4, name, normal);
        for (int i = 0; i < buckets.length; i++)
            setText(sheet, row, 5 + i, String.valueOf(buckets[i]), number);
    }

    private void setText(Sheet sheet, int row, int col, String text, CellStyle style) {
        Row r = sheet.getRow(row);
        Cell c = r.createCell(col);
        c.setCellValue(text);
        c.setCellStyle(style);
    }

    private void merge(Sheet sheet, int r1, int r2, int c1, int c2) {
        sheet.addMergedRegion(new CellRangeAddress(r1, r2, c1, c2));
    }
}*/
