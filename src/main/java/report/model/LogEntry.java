//how raw data is stored
//not in use
//using streaming aggregation
package report.model;

import java.time.LocalDateTime;

public class LogEntry {

    private LocalDateTime time;
    private boolean reqIn;
    private boolean reqOut;

    private String type;          // Normalized type (without REQ/RESP)
    private String txnStatus;     // Only for responses

    private Long val;
    private Long top;
    private Long ppt;
    private Long rtt;

    private Long activeSize;

    public LogEntry(LocalDateTime time) {
        this.time = time;
    }


    public LocalDateTime getTime() { return time; }
    public boolean isReqIn() { return reqIn; }
    public boolean isReqOut() { return reqOut; }
    public String getType() { return type; }
    public String getTxnStatus() { return txnStatus; }
    public Long getVal() { return val; }
    public Long getTop() { return top; }
    public Long getPpt() { return ppt; }
    public Long getRtt() { return rtt; }
    public Long getActiveSize() { return activeSize; }

    public void setReqIn(boolean reqIn) { this.reqIn = reqIn; }
    public void setReqOut(boolean reqOut) { this.reqOut = reqOut; }
    public void setType(String type) { this.type = type; }
    public void setTxnStatus(String txnStatus) { this.txnStatus = txnStatus; }
    public void setVal(Long val) { this.val = val; }
    public void setTop(Long top) { this.top = top; }
    public void setPpt(Long ppt) { this.ppt = ppt; }
    public void setRtt(Long rtt) { this.rtt = rtt; }
    public void setActiveSize(Long activeSize) { this.activeSize = activeSize; }
}
