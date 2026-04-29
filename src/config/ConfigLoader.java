package config;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
public class ConfigLoader {
    private Properties properties;

    public ConfigLoader() {
        properties = new Properties();
        loadProperties();
    }

    private void loadProperties() {
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            properties.load(fis);
            // 🔍 TEMPORARY DEBUG LINE
            System.out.println("Log directory from config: " +
                    properties.getProperty("log.directory"));
        } catch (IOException e) {
            throw new RuntimeException("Unable to load config.properties", e);
        }
    }

    public String getLogDirectory() {
        return properties.getProperty("log.directory");
    }
}
