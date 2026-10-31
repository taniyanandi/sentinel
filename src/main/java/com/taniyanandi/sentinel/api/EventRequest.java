package com.taniyanandi.sentinel.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.Map;

@Schema(description = "A single telemetry event submitted for ingestion and anomaly detection.")
public record EventRequest(
        @Schema(description = "Unique identifier for this event.", example = "evt-1001")
        @NotBlank String eventId,

        @Schema(description = "Origin system or service that produced the event.", example = "payments-service")
        @NotBlank String source,

        @Schema(description = "Identifier of the entity the metric belongs to.", example = "merchant-42")
        @NotBlank String entityId,

        @Schema(description = "Name of the metric being reported.", example = "latency_ms")
        @NotBlank String metric,

        @Schema(description = "Numeric value of the metric. Must be zero or positive.", example = "123.45")
        @PositiveOrZero double value,

        @Schema(description = "Timestamp at which the event occurred.", example = "2026-10-11T14:45:00Z")
        @NotNull Instant timestamp,

        @Schema(description = "Optional free-form key/value metadata attached to the event.")
        Map<String, String> attributes) {
}
