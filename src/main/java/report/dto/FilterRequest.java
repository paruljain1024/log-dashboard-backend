//Used when filtering performance metrics
package report.dto;

public class FilterRequest {

    public String metric;
    public String type;
    public String from;
    public String to;
    public int interval = 1;

}