//recursive scaning of log files
package report.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import report.service.ConfigLoader;

public class LogFileLocator {

    public static final String BASE_LOG_DIR = System.getProperty("log.base.dir", "C:/Users/parul.jain2/logs");


    public static List<File> findAllLogFiles() {

        List<File> logFiles = new ArrayList<>();

        File baseDir = new File(BASE_LOG_DIR);

        if (!baseDir.exists() || !baseDir.isDirectory()) {
            System.out.println("❌ Base log directory not found: " + BASE_LOG_DIR);
            return logFiles;
        }

        System.out.println("🔍 Scanning base directory: " + baseDir.getAbsolutePath());

        scanRecursively(baseDir, logFiles);

        System.out.println("✅ Total log files discovered: " + logFiles.size());

        return logFiles;
    }

    private static void scanRecursively(File dir, List<File> logFiles) {

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {

            if (f.isDirectory()) {
                scanRecursively(f, logFiles); // 🔁 RECURSION
            } else {
                String name = f.getName().toLowerCase();

                // ✅ Accept all required log patterns
                if (name.contains("channelgatewayrequestlog")
                        || name.contains("channelrequestdailylog")
                        || name.contains("pretups_out")
                        || name.contains("web_pretups_out")) {

                    System.out.println("   ✅ Found log file: " + f.getAbsolutePath());
                    logFiles.add(f);
                }
            }
        }
    }
}
