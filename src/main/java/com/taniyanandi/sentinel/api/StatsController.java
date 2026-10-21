package com.taniyanandi.sentinel.api;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1")
public class StatsController {

    private final MeterRegistry meterRegistry;

    // Spring Boot auto-wires MeterRegistry automatically
    public StatsController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1. Read counters safely (defaulting to 0.0 if not yet initialized)
        double eventsIngested = meterRegistry.find("sentinel.events.ingested").counter() != null
                ? meterRegistry.find("sentinel.events.ingested").counter().count() : 0;

        double anomaliesDetected = meterRegistry.find("sentinel.anomalies.detected").counter() != null
                ? meterRegistry.find("sentinel.anomalies.detected").counter().count() : 0;

        double criticalAnomalies = meterRegistry.find("sentinel.critical.anomalies").counter() != null
                ? meterRegistry.find("sentinel.critical.anomalies").counter().count() : 0;

        // 2. Compute uptime in seconds
        long uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

        // 3. Construct JSON response
        stats.put("eventsIngested", (long) eventsIngested);
        stats.put("anomaliesDetected", (long) anomaliesDetected);
        stats.put("criticalAnomalies", (long) criticalAnomalies);
        stats.put("uptimeSeconds", uptimeSeconds);

        return stats;
    }
}
