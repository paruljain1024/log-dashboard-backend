package report.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class LogFileLocator {

    @Value("${log.base.dir}")
    private String baseLogDir;

    public List<File> findAllLogFiles() {

        List<File> logFiles = new ArrayList<>();

        File baseDir = new File(baseLogDir);

        if (!baseDir.exists() || !baseDir.isDirectory()) {

            System.out.println("❌ Base log directory not found: " + baseLogDir);

            return logFiles;
        }

        System.out.println("🔍 Scanning base directory: "
                + baseDir.getAbsolutePath());

        scanRecursively(baseDir, logFiles);

        System.out.println("✅ Total log files discovered: "
                + logFiles.size());

        return logFiles;
    }

    private void scanRecursively(
            File dir,
            List<File> logFiles
    ) {

        File[] files = dir.listFiles();

        if (files == null) return;

        for (File f : files) {

            if (f.isDirectory()) {

                scanRecursively(f, logFiles);

            } else {

                String name =
                        f.getName().toLowerCase();

                if (name.contains("channelgatewayrequestlog")
                        || name.contains("channelrequestdailylog")
                        || name.contains("pretups_out")
                        || name.contains("web_pretups_out")) {

                    System.out.println(
                            "✅ Found log file: "
                                    + f.getAbsolutePath());

                    logFiles.add(f);
                }
            }
        }
    }
}