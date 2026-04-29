package report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DashboardApplication {

    public static void main(String[] args) throws Exception {

        SpringApplication.run(DashboardApplication.class, args);

        // ✅ TEMP TEST
        /*DocReportGenerator.generateDocReport(
                "C:\\Users\\parul.jain2\\logs\\charts",
                "C:\\Users\\parul.jain2\\logs\\TestReport.docx"
        );*/
    }
}
