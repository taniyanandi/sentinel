package com.taniyanandi.sentinel.service;

import com.taniyanandi.sentinel.domain.AnomalyAlert;
import com.taniyanandi.sentinel.domain.DetectionState;
import com.taniyanandi.sentinel.domain.TelemetryEvent;
import com.taniyanandi.sentinel.metrics.SentinelMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class AnomalyDetector {
    private final long warmupEvents;
    private final double zThreshold;
    private final long cooldownSeconds;
    private final SentinelMetrics metrics;
    private final Clock clock;

    @Autowired
    public AnomalyDetector(@Value("${sentinel.detector.warmup-events:30}") long warmupEvents,
                           @Value("${sentinel.detector.z-threshold:3.0}") double zThreshold,
                           @Value("${sentinel.detector.cooldown-seconds:300}") long cooldownSeconds,
                           SentinelMetrics metrics) {
        this(warmupEvents, zThreshold, cooldownSeconds, metrics, Clock.systemUTC());
    }

    AnomalyDetector(long warmupEvents, double zThreshold, long cooldownSeconds, SentinelMetrics metrics, Clock clock) {
        this.warmupEvents = warmupEvents;
        this.zThreshold = zThreshold;
        this.cooldownSeconds = cooldownSeconds;
        this.metrics = metrics;
        this.clock = clock;
    }

    public AnomalyAlert evaluate(TelemetryEvent event, DetectionState state) {
        return metrics.detectionLatency().record(() -> evaluateInternal(event, state));
    }

    private AnomalyAlert evaluateInternal(TelemetryEvent event, DetectionState state) {
        if (state.count() < warmupEvents || state.stdDev() < 1e-9) { state.observe(event.value()); return null; }
        double mean = state.mean(), stdDev = state.stdDev();
        double z = Math.abs(event.value() - mean) / stdDev; state.observe(event.value());
        if (z < zThreshold) return null;

        Instant now = Instant.now(clock);
        Instant lastAlertAt = state.lastAlertAt();
        if (cooldownSeconds > 0 && lastAlertAt != null && now.isBefore(lastAlertAt.plusSeconds(cooldownSeconds))) {
            metrics.anomalySuppressed();
            return null;
        }

        AnomalyAlert.Severity severity = z >= 6 ? AnomalyAlert.Severity.CRITICAL : z >= 5 ? AnomalyAlert.Severity.HIGH : z >= 4 ? AnomalyAlert.Severity.MEDIUM : AnomalyAlert.Severity.LOW;
        metrics.anomalyDetected(severity == AnomalyAlert.Severity.CRITICAL);
        state.markAlerted(now);
        return new AnomalyAlert(UUID.randomUUID().toString(), event.eventId(), event.entityId(), event.metric(), event.value(), mean, stdDev, z, severity,
                "Value deviated " + String.format("%.2f", z) + " standard deviations from baseline", now);
    }
}
