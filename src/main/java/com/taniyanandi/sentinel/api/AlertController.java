package com.taniyanandi.sentinel.api;

import com.taniyanandi.sentinel.persistence.AlertEntity;
import com.taniyanandi.sentinel.persistence.AlertRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Alerts", description = "Read-only access to anomaly alerts detected by Sentinel.")
@RestController
@RequestMapping("/v1/alerts")
public class AlertController {
    private final AlertRepository repository;
    public AlertController(AlertRepository repository) { this.repository = repository; }

    @Operation(
            summary = "List recent alerts",
            description = "Returns the most recently detected alerts across all entities and metrics, newest first.")
    @ApiResponse(responseCode = "200", description = "Alerts returned successfully")
    @GetMapping
    public List<AlertEntity> recent(
            @Parameter(description = "Maximum number of alerts to return (clamped between 1 and 200).", example = "50")
            @RequestParam(defaultValue = "50") int limit) {
        return repository.findByOrderByDetectedAtDesc(PageRequest.of(0, Math.min(Math.max(limit, 1), 200)));
    }

    @Operation(
            summary = "List recent alerts by severity",
            description = "Returns the most recently detected alerts filtered by severity, newest first.")
    @ApiResponse(responseCode = "200", description = "Alerts returned successfully")
    @GetMapping("/severity/{severity}")
    public List<AlertEntity> bySeverity(
            @Parameter(description = "Severity level to filter by.") @PathVariable AlertEntity.Severity severity,
            @Parameter(description = "Maximum number of alerts to return (clamped between 1 and 200).", example = "50")
            @RequestParam(defaultValue = "50") int limit) {
        return repository.findBySeverityOrderByDetectedAtDesc(severity, PageRequest.of(0, Math.min(Math.max(limit, 1), 200)));
    }
}
