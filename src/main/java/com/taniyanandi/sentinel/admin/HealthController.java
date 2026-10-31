package com.taniyanandi.sentinel.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;
import java.util.Map;

@Tag(name = "System", description = "Service metadata and health information.")
@RestController
public class HealthController {
    @Operation(
            summary = "Get service info",
            description = "Returns basic metadata about the running Sentinel instance (name, version, mode, current time).")
    @ApiResponse(responseCode = "200", description = "Service info returned successfully")
    @GetMapping("/v1/system/info")
    public Map<String, Object> info() {
        return Map.of("service", "sentinel", "version", "2.0.0", "mode", "streaming", "timestamp", Instant.now());
    }
}
