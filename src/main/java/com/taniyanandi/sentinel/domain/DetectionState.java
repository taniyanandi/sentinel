package com.taniyanandi.sentinel.domain;

import java.io.Serializable;
import java.time.Instant;

public class DetectionState implements Serializable {
    private long count;
    private double mean;
    private double m2;
    private Long lastAlertAtEpochMilli;

    public DetectionState() {}

    public void observe(double value) {
        count++;
        double delta = value - mean;
        mean += delta / count;
        double delta2 = value - mean;
        m2 += delta * delta2;
    }

    public long count() { return count; }
    public double mean() { return mean; }
    public double variance() { return count > 1 ? m2 / (count - 1) : 0.0; }
    public double stdDev() { return Math.sqrt(variance()); }

    /** Timestamp of the last alert emitted for this key, or {@code null} if none has been emitted yet. */
    public Instant lastAlertAt() {
        return lastAlertAtEpochMilli == null ? null : Instant.ofEpochMilli(lastAlertAtEpochMilli);
    }

    /** Records that an alert was just emitted for this key, starting (or restarting) its cooldown window. */
    public void markAlerted(Instant instant) {
        this.lastAlertAtEpochMilli = instant.toEpochMilli();
    }

    public Long getLastAlertAtEpochMilli() { return lastAlertAtEpochMilli; }
    public void setLastAlertAtEpochMilli(Long lastAlertAtEpochMilli) { this.lastAlertAtEpochMilli = lastAlertAtEpochMilli; }
}
