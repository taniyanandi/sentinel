package com.taniyanandi.sentinel.domain;

import java.io.Serializable;

public class DetectionState implements Serializable {
    private long count;
    private double mean;
    private double m2;

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

    public long getCount() { return count; }
    public double getMean() { return mean; }
    public double getM2() { return m2; }

    public void setCount(long count) { this.count = count; }
    public void setMean(double mean) { this.mean = mean; }
    public void setM2(double m2) { this.m2 = m2; }
}
