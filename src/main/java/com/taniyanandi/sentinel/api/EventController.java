package com.taniyanandi.sentinel.api;

import com.taniyanandi.sentinel.domain.TelemetryEvent;
import com.taniyanandi.sentinel.service.EventIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Tag(name = "Events", description = "Ingestion endpoint for telemetry events used by the anomaly detection pipeline.")
@RestController
@RequestMapping("/v1/events")
public class EventController {
    private final EventIngestionService ingestionService;

    public EventController(EventIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Operation(
            summary = "Ingest a telemetry event",
            description = "Accepts a telemetry event and publishes it for downstream anomaly detection processing.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Event accepted for processing"),
            @ApiResponse(responseCode = "400", description = "Event payload failed validation")
    })
    @PostMapping
    public ResponseEntity<Void> ingest(@Valid @RequestBody EventRequest request) {
        ingestionService.publish(new TelemetryEvent(
                request.eventId(), request.source(), request.entityId(), request.metric(),
                request.value(), request.timestamp(), request.attributes()));
        return ResponseEntity.accepted().location(URI.create("/v1/events/" + request.eventId())).build();
    }
}
