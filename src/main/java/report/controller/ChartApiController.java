//customized graphs

package report.controller;

import org.springframework.web.bind.annotation.*;
import report.dto.ChartPoint;
import report.service.ChartDataService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chart")
@CrossOrigin("*")
public class ChartApiController {

    private final ChartDataService service;

    public ChartApiController(ChartDataService service) {
        this.service = service;
    }

    @GetMapping("/{metric}")
    public List<ChartPoint> getChart(
            @PathVariable String metric) {

        return service.getMetric(metric);
    }
    @GetMapping("/custom")
    public List<ChartPoint> customChart(
            @RequestParam String metric,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "1") int interval
    ) {

        return service.getCustomMetric(
                metric,
                date,
                from,
                to,
                interval
        );
    }

    @GetMapping("/typewise-timeseries")
    public Map<String, Object> getTypewiseTimeSeries() {
        return service.getTypewiseTimeSeries();
    }

}