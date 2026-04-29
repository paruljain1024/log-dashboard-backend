//not in use
/*package report;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import java.io.*;
import java.math.BigInteger;
import java.util.List;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import java.math.BigInteger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import java.awt.*;
import java.io.FileInputStream;
import org.apache.poi.ooxml.POIXMLDocumentPart;

public class DocReportGenerator {

    public static void generateDocReport(String chartDir, String outputPath) throws Exception {

        XWPFDocument doc = new XWPFDocument();

        CTSectPr sectPr = doc.getDocument().getBody().addNewSectPr();
        CTPageSz pageSize = sectPr.addNewPgSz();
        pageSize.setOrient(STPageOrientation.LANDSCAPE);
        pageSize.setW(BigInteger.valueOf(25000)); // Width
        pageSize.setH(BigInteger.valueOf(12240)); // Height
        CTPageMar pageMar = sectPr.addNewPgMar();
        pageMar.setLeft(BigInteger.valueOf(720));
        pageMar.setRight(BigInteger.valueOf(720));
        pageMar.setTop(BigInteger.valueOf(720));
        pageMar.setBottom(BigInteger.valueOf(720));

        File chartBase = new File(chartDir);

        if (!chartBase.exists()) {
            System.out.println("Charts directory not found: " + chartDir);
            return;
        }

        String runPath = chartBase.getAbsolutePath();

        System.out.println("✅ Using charts from: " + runPath);

        addTitle(doc, "Load Test Graphical Report");

        addFourChartsPage(doc,
                getLatestChart(runPath, "request_in").getAbsolutePath(), "Request IN (TPS Trend)",
                getLatestChart(runPath, "request_out").getAbsolutePath(), "Request OUT Trend",
                getLatestChart(runPath, "active_size").getAbsolutePath(), "Active Size Trend",
                getLatestChart(runPath, "efficiency").getAbsolutePath(), "Efficiency Trend (Success vs Failure)");


        addFourChartsPage(doc,
                getLatestChart(runPath, "ppt").getAbsolutePath(), "PPT Trend",
                getLatestChart(runPath, "top").getAbsolutePath(), "TOP Trend",
                getLatestChart(runPath, "val").getAbsolutePath(), "VAL Trend",
                getLatestChart(runPath, "rtt").getAbsolutePath(), "RTT Trend");


        try (FileOutputStream out = new FileOutputStream(outputPath)) {
            doc.write(out);
        }

        doc.close();
    }

    private static void addTitle(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(18);
        run.addBreak();
    }


    private static void addSection(XWPFDocument doc, String heading) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(400);   // space before section
        p.setSpacingAfter(200);    // space after heading

        XWPFRun run = p.createRun();
        run.setBold(true);
        run.setFontSize(14);
        run.setText(heading);
    }


    private static void addImage(XWPFDocument doc, String imagePath) throws Exception {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);

        XWPFRun run = p.createRun();
        try (FileInputStream is = new FileInputStream(imagePath)) {
            run.addPicture(
                    is,
                    Document.PICTURE_TYPE_PNG,
                    imagePath,
                    org.apache.poi.util.Units.toEMU(500),
                    org.apache.poi.util.Units.toEMU(300)
            );
        }

    }
    private static void styleChart(JFreeChart chart, Color lineColor) {
        chart.setBackgroundPaint(Color.WHITE);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(new Color(245, 247, 250)); // light grey background
        plot.setRangeGridlinePaint(Color.GRAY);

        LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, lineColor);
        renderer.setSeriesStroke(0, new BasicStroke(2.5f));
    }
    private static void addChartCell(XWPFTableCell cell, File imageFile, String title) throws Exception {

        cell.removeParagraph(0);

        // ===== TITLE ABOVE CHART =====
        XWPFParagraph titlePara = cell.addParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);

        XWPFRun titleRun = titlePara.createRun();
        titleRun.setBold(true);
        titleRun.setFontSize(13);
        titleRun.setText(title);

        // ===== IMAGE BELOW TITLE =====
        XWPFParagraph para = cell.addParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);

        XWPFRun run = para.createRun();

        try (FileInputStream is = new FileInputStream(imageFile)) {
            run.addPicture(
                    is,
                    Document.PICTURE_TYPE_PNG,
                    imageFile.getName(),
                    Units.toEMU(260),   // width
                    Units.toEMU(150)    // height
            );
        }
    }





    private static void addFourChartsPage(XWPFDocument doc,
                                          String img1, String title1,
                                          String img2, String title2,
                                          String img3, String title3,
                                          String img4, String title4) throws Exception {

        // ---- CREATE TABLE WITH 4 CHARTS ----
        XWPFTable table = doc.createTable(2, 2);
        table.setWidth("100%");

        table.setCellMargins(200, 200, 200, 200);
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                XWPFParagraph p = cell.getParagraphs().get(0);
                p.setSpacingBefore(100);
                p.setSpacingAfter(100);
            }
        }



        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                cell.getCTTc().addNewTcPr().addNewTcBorders().addNewTop().setVal(STBorder.NONE);
                cell.getCTTc().getTcPr().getTcBorders().addNewBottom().setVal(STBorder.NONE);
                cell.getCTTc().getTcPr().getTcBorders().addNewLeft().setVal(STBorder.NONE);
                cell.getCTTc().getTcPr().getTcBorders().addNewRight().setVal(STBorder.NONE);
            }
        }
        table.getRow(0).getCell(0).getCTTc().addNewTcPr().addNewTcW().setW(BigInteger.valueOf(5000));
        table.getRow(0).getCell(1).getCTTc().addNewTcPr().addNewTcW().setW(BigInteger.valueOf(5000));
        table.getRow(1).getCell(0).getCTTc().addNewTcPr().addNewTcW().setW(BigInteger.valueOf(5000));
        table.getRow(1).getCell(1).getCTTc().addNewTcPr().addNewTcW().setW(BigInteger.valueOf(5000));



        addChartCell(table.getRow(0).getCell(0), new File(img1), title1);
        addChartCell(table.getRow(0).getCell(1), new File(img2), title2);
        addChartCell(table.getRow(1).getCell(0), new File(img3), title3);
        addChartCell(table.getRow(1).getCell(1), new File(img4), title4);

        // ===== REMARKS FOR TRAFFIC =====
        /*addRemarksSection(doc,
                "Traffic Observation",
                "• Request IN and OUT show a stable ramp-up during load.\n" +
                        "• Minor fluctuations are expected under high TPS and are within acceptable limits."
        );

// ===== REMARKS FOR SYSTEM =====
        addRemarksSection(doc,
                "System Behaviour",
                "• Active connections remain within expected operating range.\n" +
                        "• No abnormal spikes observed indicating resource exhaustion.\n" +
                        "• Success rate is high, and failures are negligible."
        );


        doc.createParagraph().setPageBreak(true); // Next page

    }
    private static void addRemarksSection(XWPFDocument doc, String heading, String text) {

        XWPFParagraph headingPara = doc.createParagraph();
        headingPara.setSpacingBefore(300);

        XWPFRun headingRun = headingPara.createRun();
        headingRun.setText("Remarks: " + heading);
        headingRun.setBold(true);
        headingRun.setFontSize(13);

        XWPFParagraph textPara = doc.createParagraph();
        XWPFRun textRun = textPara.createRun();
        textRun.setText(text);
        textRun.setFontSize(11);
    }
    private static File getLatestChart(String chartDir, String prefix) {

        File dir = new File(chartDir);

        if (!dir.exists()) {
            System.out.println("Charts directory not found: " + chartDir);
            return null;
        }

        File[] files = dir.listFiles((d, name) ->
                name.startsWith(prefix) && name.endsWith(".png"));

        if (files == null || files.length == 0) {
            System.out.println("No charts found for: " + prefix);
            return null;
        }

        File latest = files[0];

        for (File f : files) {
            if (f.lastModified() > latest.lastModified()) {
                latest = f;
            }
        }

        return latest;
    }
//latest folder finder
    private static File getLatestRunFolder(String baseDir) {

        File base = new File(baseDir);

        File[] dirs = base.listFiles(File::isDirectory);

        if (dirs == null || dirs.length == 0)
        { System.out.println("Charts directory not found: " + dirs);
        return null;}

        File latest = dirs[0];

        for (File d : dirs) {
            if (d.lastModified() > latest.lastModified()) {
                latest = d;
            }
        }

        return latest;
    }








}*/
