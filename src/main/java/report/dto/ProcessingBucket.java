//This represents processing time distribution.
package report.dto;

public class ProcessingBucket {

    public String range;
    public long count;

    public ProcessingBucket(String range, long count) {
        this.range = range;
        this.count = count;
    }
}