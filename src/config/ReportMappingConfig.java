package config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class ReportMappingConfig {

    private final Properties props = new Properties();

    public ReportMappingConfig(String filePath) {
        try (InputStream in = new FileInputStream(filePath)) {
            props.load(in);
        } catch (Exception e) {
            System.out.println("Mapping config not found. Using default type names.");
        }
    }

    public String displayNameForType(String type) {
        if (type == null) return "UNKNOWN";
        return props.getProperty("type." + type, type);
    }
}
