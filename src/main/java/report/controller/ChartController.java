//normal graphs
package report.controller;

import org.springframework.core.io.*;
import org.springframework.web.bind.annotation.*;

import java.nio.file.*;

@RestController
@RequestMapping("/charts")
public class ChartController {

    private final String chartBase =
            System.getProperty("user.home") + "/logs/charts";

    @GetMapping("/{fileName}")
    public Resource getChart(@PathVariable String fileName)
            throws Exception {

        Path path = Paths.get(chartBase, fileName);

        return new UrlResource(path.toUri());
    }
}