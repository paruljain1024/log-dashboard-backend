//this represents processing time distribution per transaction type.
package report.dto;

import java.util.List;

public class ProcessingTimeResponse {

    public String type;
    public List<ProcessingBucket> buckets;

    public ProcessingTimeResponse(
            String type,
            List<ProcessingBucket> buckets) {

        this.type = type;
        this.buckets = buckets;
    }
}