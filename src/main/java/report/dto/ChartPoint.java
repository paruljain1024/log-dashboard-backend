//dto=data transfer object
//dto class= bridge between frontend and backend
//They define exactly what JSON structure the frontend receives


//universal graph format
package report.dto;

public class ChartPoint {

    public String date;
    public String time;
    public Double value;

    public Double min;
    public Double avg;
    public Double max;

    public ChartPoint(String date, String time, Number value) {
        this.date = date;
        this.time = time;
        this.value = value.doubleValue();
    }

    public ChartPoint(String date,
                      String time,
                      double min,
                      double avg,
                      double max) {

        this.date = date;
        this.time = time;
        this.min = min;
        this.avg = avg;
        this.max = max;
    }
}