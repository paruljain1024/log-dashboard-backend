//excel apis
package report.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import report.service.ExcelDataService;

@RestController
@RequestMapping("/api/excel")
@CrossOrigin("*")
public class ExcelController {

    private final ExcelDataService service;

    public ExcelController(ExcelDataService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public Object summary() {
        return service.getSummary();
    }

    @GetMapping("/types")
    public Object types() {
        return service.getTypeStats();
    }

    @GetMapping("/response-codes")
    public Object responseCodes() {
        return service.getResponseCodes();
    }

    @GetMapping("/performance")
    public Object performance() {
        return service.getPerformance();
    }

    @GetMapping("/processing-time")
    public Object processingTime() {
        return service.getProcessingTime();
    }

    @GetMapping("/transaction-time")
    public Object transactionTime() {
        return service.getTransactionTime();
    }

    @GetMapping("/active-size")
    public Object activeSize() {
        return service.getActiveSize();
    }

    @GetMapping("/download-formatted")
    public ResponseEntity<byte[]> downloadFormatted() throws Exception {

        byte[] file = service.generateFullReport();

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=Formatted_Report.xlsx")
                .body(file);
    }
}
