package com.taniyanandi.sentinel.domain;

import java.time.Instant;

public record DeadLetterEvent(
        TelemetryEvent event,
        String errorType,
        String errorMessage,
        Instant failedAt) {
}
