package report.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Service
public class TypeNameMapper {

    private final Properties defaultProperties = new Properties();
    private volatile Properties effectiveProperties = new Properties();
    private final Path externalFilePath;

    private volatile boolean externalFilePresent = false;
    private volatile long externalFileLastModified = -1L;

    public TypeNameMapper(
            @Value("${type.mapping.file:./config/type-mapping.properties}")
            String fileName
    ) {
        this.externalFilePath = resolvePath(fileName);
        loadClasspathDefaults();
        reloadMappings();
    }

    public String map(String type) {
        refreshMappingsIfChanged();
        return effectiveProperties.getProperty(type, type);
    }

    public String getDisplayName(String type) {
        return map(type);
    }

    private void refreshMappingsIfChanged() {
        File externalFile = externalFilePath.toFile();
        boolean present = externalFile.isFile();
        long modified = present ? externalFile.lastModified() : -1L;

        if (present == externalFilePresent && modified == externalFileLastModified) {
            return;
        }

        synchronized (this) {
            File currentFile = externalFilePath.toFile();
            boolean currentPresent = currentFile.isFile();
            long currentModified = currentPresent ? currentFile.lastModified() : -1L;

            if (currentPresent != externalFilePresent
                    || currentModified != externalFileLastModified) {
                reloadMappings();
            }
        }
    }

    private void reloadMappings() {
        Properties merged = new Properties();
        merged.putAll(defaultProperties);

        File externalFile = externalFilePath.toFile();

        if (externalFile.isFile()) {
            try (FileInputStream fis = new FileInputStream(externalFile)) {
                Properties externalProperties = new Properties();
                externalProperties.load(fis);
                merged.putAll(externalProperties);
                externalFilePresent = true;
                externalFileLastModified = externalFile.lastModified();
                System.out.println("Loaded external type mapping from: " + externalFilePath);
            } catch (IOException ignored) {
                externalFilePresent = false;
                externalFileLastModified = -1L;
                System.out.println("Failed to load external type mapping from: " + externalFilePath);
            }
        } else {
            externalFilePresent = false;
            externalFileLastModified = -1L;
            System.out.println("External type mapping not found at: " + externalFilePath
                    + ". Using packaged defaults.");
        }

        effectiveProperties = merged;
    }

    private void loadClasspathDefaults() {
        try (InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream("type-mapping.properties")) {
            if (inputStream == null) {
                return;
            }

            defaultProperties.load(inputStream);
        } catch (IOException ignored) {
        }
    }

    private Path resolvePath(String fileName) {
        Path configuredPath = Paths.get(fileName);

        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }

        return Paths.get(System.getProperty("user.dir"))
                .resolve(configuredPath)
                .normalize();
    }
}
