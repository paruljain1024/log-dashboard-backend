/*package report.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "metrics")
public class MetricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long timestamp;

    private int requestIn;
    private int requestOut;
    private int success;
    private int failure;

    private long activeSize;

    private String type;

    // getters and setters

    public Long getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getRequestIn() {
        return requestIn;
    }

    public void setRequestIn(int requestIn) {
        this.requestIn = requestIn;
    }

    public int getRequestOut() {
        return requestOut;
    }

    public void setRequestOut(int requestOut) {
        this.requestOut = requestOut;
    }

    public int getSuccess() {
        return success;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public int getFailure() {
        return failure;
    }

    public void setFailure(int failure) {
        this.failure = failure;
    }

    public long getActiveSize() {
        return activeSize;
    }

    public void setActiveSize(long activeSize) {
        this.activeSize = activeSize;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}*/