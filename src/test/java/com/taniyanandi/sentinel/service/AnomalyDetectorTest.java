package com.taniyanandi.sentinel.service;

import com.taniyanandi.sentinel.domain.AnomalyAlert;
import com.taniyanandi.sentinel.domain.DetectionState;
import com.taniyanandi.sentinel.domain.TelemetryEvent;
import com.taniyanandi.sentinel.metrics.SentinelMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnomalyDetectorTest {
    @Test
    void emitsCriticalAlertForLargeDeviation() {
        SentinelMetrics metrics = new SentinelMetrics(new SimpleMeterRegistry());
        AnomalyDetector detector = new AnomalyDetector(5, 3.0, 300, metrics, Clock.systemUTC());
        DetectionState state = new DetectionState();

        for (int i = 0; i < 5; i++) {
            detector.evaluate(event(i + 1), state);
        }

        AnomalyAlert alert = detector.evaluate(event(100), state);

        assertThat(alert).isNotNull();
        assertThat(alert.severity()).isIn(AnomalyAlert.Severity.HIGH, AnomalyAlert.Severity.CRITICAL);
        assertThat(alert.zScore()).isGreaterThan(3.0);
    }

    @Test
    void suppresssRepeatedAlertWithinCooldownWindow() {
        SentinelMetrics metrics = new SentinelMetrics(new SimpleMeterRegistry());
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        MutableClock clock = new MutableClock(base);
        AnomalyDetector detector = new AnomalyDetector(5, 3.0, 300, metrics, clock);
        DetectionState state = new DetectionState();

        for (int i = 0; i < 5; i++) {
            detector.evaluate(event(i + 1), state);
        }

        AnomalyAlert first = detector.evaluate(event(100), state);
        assertThat(first).isNotNull();

        clock.advanceSeconds(60); // still within the 300s cooldown
        AnomalyAlert second = detector.evaluate(event(1000), state);
        assertThat(second).isNull();
    }

    @Test
    void emitsAlertAgainAfterCooldownExpires() {
        SentinelMetrics metrics = new SentinelMetrics(new SimpleMeterRegistry());
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        MutableClock clock = new MutableClock(base);
        AnomalyDetector detector = new AnomalyDetector(5, 3.0, 300, metrics, clock);
        DetectionState state = new DetectionState();

        for (int i = 0; i < 5; i++) {
            detector.evaluate(event(i + 1), state);
        }

        AnomalyAlert first = detector.evaluate(event(100), state);
        assertThat(first).isNotNull();

        clock.advanceSeconds(301); // past the 300s cooldown
        AnomalyAlert second = detector.evaluate(event(1000), state);
        assertThat(second).isNotNull();
    }

    private TelemetryEvent event(double value) {
        return new TelemetryEvent("e-" + value, "test", "entity-1", "latency_ms", value, Instant.now(), Map.of());
    }

    /** A fixed, manually advanceable Clock for deterministic cooldown testing. */
    private static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) { this.instant = instant; }

        void advanceSeconds(long seconds) { this.instant = this.instant.plusSeconds(seconds); }

        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
