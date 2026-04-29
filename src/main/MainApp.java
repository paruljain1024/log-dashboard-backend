package main;

import report.ChartGenerator;
import report.DocReportGenerator;
import metrics.MetricsCalculator;
import metrics.MetricsResult;
import metrics.TypeStats;
import model.LogEntry;
import parser.LogParserTask;
import report.ExcelReportGenerator;
import util.TypeNameMapper;
import util.LogFileLocator;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

import util.ConfigLoader;

import static util.LogFileLocator.BASE_LOG_DIR;

import java.text.SimpleDateFormat;
import java.util.Date;


public class MainApp {

    public static void main(String[] args) {

        if (args.length > 0 && args[0].equalsIgnoreCase("build-summary")) {

            System.out.println("MODE 1: Building Summary File Only...");

            try {
                List<File> logFiles = util.LogFileLocator.findAllLogFiles();

                if (logFiles.isEmpty()) {
                    System.out.println("No log files found.");
                    return;
                }

                List<LogEntry> parsedEntries =
                        Collections.synchronizedList(new ArrayList<>());

                ExecutorService executor =
                        Executors.newFixedThreadPool(Math.min(4, logFiles.size()));

                for (File file : logFiles) {
                    executor.submit(new parser.LogParserTask(file, parsedEntries));
                }

                executor.shutdown();
                executor.awaitTermination(10, TimeUnit.MINUTES);

                System.out.println("Summary file generated successfully.");
                System.out.println("Total parsed entries: " + parsedEntries.size());

            } catch (Exception e) {
                e.printStackTrace();
            }

            return; // 🔥 STOP HERE (no charts, no excel)
        }


        String timeTag = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());


        try {
            List<File> logFiles = LogFileLocator.findAllLogFiles();
            if (logFiles.isEmpty()) {
                System.out.println("❌ No log files found.");
                return;
            }

            System.out.println("✅ Total log files found: " + logFiles.size());

            List<LogEntry> parsedEntries = Collections.synchronizedList(new ArrayList<>());
            ExecutorService executor = Executors.newFixedThreadPool(Math.min(4, logFiles.size()));

            for (File file : logFiles) {
                executor.submit(new LogParserTask(file, parsedEntries));
            }

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.MINUTES);

            System.out.println("📄 Total parsed entries: " + parsedEntries.size());

            MetricsCalculator calculator = new MetricsCalculator();
            MetricsResult result = calculator.calculate(parsedEntries);

            System.out.println("\n===== METRICS SUMMARY =====");
            for (String type : result.typeStatsMap.keySet()) {
                TypeStats s = result.typeStatsMap.get(type);
                System.out.println("TYPE: " + type +
                        " | Fired=" + s.totalRequests +
                        " | Received=" + s.requestReceived +
                        " | Success=" + s.successCount);
            }

            TypeNameMapper mapper = new TypeNameMapper("type-mapping.properties");

            ExcelReportGenerator excelGenerator = new ExcelReportGenerator();



            String excelPathStr = ConfigLoader.get("excel.output", "LoadTestReport.xlsx");
            excelPathStr = excelPathStr.replace(".xlsx", "") + "_" + timeTag + ".xlsx";
            Path outputPath;
            if (Paths.get(excelPathStr).isAbsolute()) {
                outputPath = Paths.get(excelPathStr); // Server full path
            } else {
                outputPath = Paths.get(BASE_LOG_DIR, excelPathStr); // IntelliJ local
            }


            excelGenerator.generateExcelReport(result, outputPath, mapper);
            System.out.println("✅ Excel report generated!");

            String chartDirStr = ConfigLoader.get("chart.folder", "charts");

            String chartDir;
            if (Paths.get(chartDirStr).isAbsolute()) {
                chartDir = chartDirStr;
            } else {
                chartDir = Paths.get(BASE_LOG_DIR, chartDirStr).toString();
            }
            new File(chartDir).mkdirs();

            // ⭐ Unique timestamp for all generated files
            //String timeTag = String.valueOf(System.currentTimeMillis());

            /*System.out.println("RequestIn time keys: " + result.requestInPerSec.keySet());
            System.out.println("ActiveSize time keys: " + result.activeSizePerSec.keySet());*/


            ChartGenerator.generateRequestInChart(result, chartDir, "request_in_" + timeTag + ".png");
            ChartGenerator.generateRequestOutChart(result, chartDir, "request_out_" + timeTag + ".png");
            ChartGenerator.generateActiveSizeChart(result, chartDir, "active_size_" + timeTag + ".png");

            ChartGenerator.generateMinAvgMaxChart(result.pptPerSec,
                    "PPT Trend", "PPT (ms)", chartDir, "ppt_" + timeTag + ".png");

            ChartGenerator.generateMinAvgMaxChart(result.topPerSec,
                    "TOP Trend", "TOP (ms)",  chartDir,"top_" + timeTag + ".png");

            ChartGenerator.generateMinAvgMaxChart(result.valPerSec,
                    "VAL Trend", "VAL (ms)",  chartDir, "val_" + timeTag + ".png");

            ChartGenerator.generateMinAvgMaxChart(result.rttPerSec,
                    "RTT Trend", "RTT (ms)", chartDir, "rtt_" + timeTag + ".png");

            ChartGenerator.generateSuccessFailureChart(result, chartDir, "success_failure_" + timeTag + ".png");
            ChartGenerator.generateEfficiencyTrendChart(result, chartDir, "efficiency_" + timeTag + ".png");

            System.out.println("📊 All charts generated!");



            String docPathStr =  ConfigLoader.get("doc.output", "LoadTestGraphicalReport.docx");
            docPathStr = docPathStr.replace(".docx", "") + "_" + timeTag + ".docx";

            String docPath;
            if (Paths.get(docPathStr).isAbsolute()) {
                docPath = docPathStr;
            } else {
                docPath = Paths.get(BASE_LOG_DIR, docPathStr).toString();
            }


            DocReportGenerator.generateDocReport(chartDir, docPath);

            System.out.println("📄 DOC Report generated at: " + docPath);

        } catch (Exception e) {
            System.out.println("❌ Error in MainApp:");
            e.printStackTrace();
        }
    }
}
