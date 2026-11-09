package com.taniyanandi.sentinel.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.Map;

public record EventRequest(
        @NotBlank String eventId,
        @NotBlank String source,
        @NotBlank String entityId,
        @NotBlank String metric,
        @NotNull @PositiveOrZero Double value,
        @NotNull Instant timestamp,
        Map<String, String> attributes) {
}
