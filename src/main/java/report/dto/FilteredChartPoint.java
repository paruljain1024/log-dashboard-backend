
//for min,max,avg values of performance metrics
package report.dto;

public class FilteredChartPoint {

    public double min;
    public double avg;
    public double max;

    public FilteredChartPoint(
            double min,
            double avg,
            double max) {
        this.min = min;
        this.avg = avg;
        this.max = max;
    }
}