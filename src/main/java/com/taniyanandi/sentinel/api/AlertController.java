package com.taniyanandi.sentinel.api;

import com.taniyanandi.sentinel.persistence.AlertEntity;
import com.taniyanandi.sentinel.persistence.AlertRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/v1/alerts")
public class AlertController {
    private final AlertRepository repository;
    public AlertController(AlertRepository repository) { this.repository = repository; }

    @GetMapping
    public List<AlertEntity> recent(@RequestParam(defaultValue = "50") int limit) {
        return repository.findByOrderByDetectedAtDesc(PageRequest.of(0, Math.min(Math.max(limit, 1), 200)));
    }

    @GetMapping("/severity/{severity}")
    public List<AlertEntity> bySeverity(@PathVariable AlertEntity.Severity severity,
                                        @RequestParam(defaultValue = "50") int limit) {
        return repository.findBySeverityOrderByDetectedAtDesc(severity, PageRequest.of(0, Math.min(Math.max(limit, 1), 200)));
    }

    @GetMapping("/{alertId}")
    public AlertEntity getById(@PathVariable String alertId) {
        return repository.findById(alertId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
