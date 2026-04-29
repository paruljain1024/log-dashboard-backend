// config properties
package report.service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {

    private static final Properties props = new Properties();

    static {
        try {
            // 1️⃣ Try loading from external file (same folder as JAR)
            InputStream external = new FileInputStream("config.properties");
            props.load(external);
            System.out.println("✅ Loaded external config.properties");
        } catch (Exception e) {
            try {
                // 2️⃣ Fallback to classpath (IntelliJ run)
                InputStream internal = ConfigLoader.class
                        .getClassLoader()
                        .getResourceAsStream("config.properties");

                if (internal != null) {
                    props.load(internal);
                    System.out.println("✅ Loaded internal config.properties");
                } else {
                    System.out.println("⚠ No config.properties found. Using defaults.");
                }

            } catch (Exception ex) {
                System.out.println("⚠ Failed to load config file.");
            }
        }
    }

    public static String get(String key, String def) {
        return props.getProperty(key, def);
    }
}
