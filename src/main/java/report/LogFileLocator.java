package report;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.FileAppender;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class LogFileLocator {

    public static Path getLogFileLocation() {

        Logger rootLogger =
                (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

        FileAppender<?> appender =
                (FileAppender<?>) rootLogger.getAppender("FILE");

        if (appender != null) {
            String fileName = appender.getFile();
            return Paths.get(fileName).getParent().toAbsolutePath();
        }

        return null;
    }
}