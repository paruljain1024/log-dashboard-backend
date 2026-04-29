package report.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


public class TypeNameMapper {

    private final Properties properties = new Properties();

    public TypeNameMapper(String fileName) {
        try (FileInputStream fis = new FileInputStream(fileName)) {
            properties.load(fis);
            System.out.println("✅ Loaded type mapping from " + fileName);
        } catch (IOException e) {
            System.out.println("⚠ No type-mapping.properties found, using raw TYPE names");
        }
    }

    // existing method
    public String map(String type) {
        return properties.getProperty(type, type);
    }

    // ⭐ ADD THIS METHOD
    public String getDisplayName(String type) {
        return map(type);
    }
}
