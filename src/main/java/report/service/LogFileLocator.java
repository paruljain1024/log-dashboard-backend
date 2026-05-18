package report.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class LogFileLocator {

    @Value("${log.base.dir}")
    private String baseLogDir;

    public List<File> findAllLogFiles() {

        List<File> logFiles = new ArrayList<>();

        File baseDir = new File(baseLogDir);

        if (!baseDir.exists() || !baseDir.isDirectory()) {
            return logFiles;
        }

        scanRecursively(baseDir, logFiles);
        logFiles.sort(Comparator.comparing(File::getAbsolutePath));
        return logFiles;
    }

    private void scanRecursively(
            File dir,
            List<File> logFiles
    ) {

        File[] files = dir.listFiles();

        if (files == null) {
            return;
        }

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
                    logFiles.add(f);
                }
            }
        }
    }
}
