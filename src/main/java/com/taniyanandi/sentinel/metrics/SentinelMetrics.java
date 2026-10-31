package com.taniyanandi.sentinel.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class SentinelMetrics {
    private final Counter ingested;
    private final Counter anomalies;
    private final Counter criticalAnomalies;
    private final Counter suppressedAnomalies;
    private final Timer detectionLatency;

    public SentinelMetrics(MeterRegistry registry) {
        ingested = Counter.builder("sentinel.events.ingested").description("Events accepted by the ingestion API").register(registry);
        anomalies = Counter.builder("sentinel.anomalies.detected").description("Anomalies emitted by the detector").register(registry);
        criticalAnomalies = Counter.builder("sentinel.anomalies.critical").description("Critical anomalies emitted").register(registry);
        suppressedAnomalies = Counter.builder("sentinel.anomalies.suppressed").description("Anomalies suppressed by the alert cooldown").register(registry);
        detectionLatency = Timer.builder("sentinel.detection.latency").description("Anomaly detection evaluation latency").register(registry);
    }
    public void eventIngested() { ingested.increment(); }
    public void anomalyDetected(boolean critical) { anomalies.increment(); if (critical) criticalAnomalies.increment(); }
    public void anomalySuppressed() { suppressedAnomalies.increment(); }
    public Timer detectionLatency() { return detectionLatency; }
}
