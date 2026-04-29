package report.metrics;

public class SpikeConfig {
    public long fixedThresholdMs = 2000;   // Rule 1: fixed threshold spike
    public int baselineWindowSize = 20;    // Rule 2: baseline window
    public double deviationFactor = 2.0;   // Rule 2: deviation factor
}
