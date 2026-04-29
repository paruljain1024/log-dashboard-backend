package report.service;

import org.springframework.stereotype.Service;
import report.metrics.MetricsResult;

import java.io.*;

@Service
public class BackupService {

    private static final String BACKUP_FILE = "metrics_backup.bin";
    private static final String CHECKPOINT_FILE = "checkpoint.txt";

    /* ===============================
        SAVE BACKUP
    =============================== */

    public void save(MetricsResult result, long lineNumber) {

        try {

            // save metrics
            ObjectOutputStream out =
                    new ObjectOutputStream(
                            new FileOutputStream(BACKUP_FILE));

            out.writeObject(result);
            out.close();

             // save checkpoint
            BufferedWriter writer =
                    new BufferedWriter(
                            new FileWriter(CHECKPOINT_FILE));

            writer.write(String.valueOf(lineNumber));
            writer.close();

            System.out.println("💾 Backup saved at line: " + lineNumber);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ===============================
        LOAD BACKUP
    =============================== */

    public MetricsResult loadMetrics() {

        try {
            File file = new File(BACKUP_FILE);

            if (!file.exists()) return null;

            ObjectInputStream in =
                    new ObjectInputStream(
                            new FileInputStream(file));

            MetricsResult result =
                    (MetricsResult) in.readObject();

            in.close();

            System.out.println("✅ Backup loaded");

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* ===============================
        LOAD CHECKPOINT
    =============================== */

    public long loadCheckpoint() {

        try {
            File file = new File(CHECKPOINT_FILE);

            if (!file.exists()) return 0;

            BufferedReader reader =
                    new BufferedReader(
                            new FileReader(file));

            long line = Long.parseLong(reader.readLine());

            reader.close();

            System.out.println("📍 Resume from line: " + line);

            return line;

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /* ===============================
        CLEAR BACKUP (OPTIONAL)
    =============================== */

    public void clear() {
        new File(BACKUP_FILE).delete();
        new File(CHECKPOINT_FILE).delete();
        System.out.println("🗑 Backup cleared");
    }
}