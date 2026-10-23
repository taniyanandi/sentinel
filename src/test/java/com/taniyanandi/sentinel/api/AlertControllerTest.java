package com.taniyanandi.sentinel.api;

import com.taniyanandi.sentinel.persistence.AlertEntity;
import com.taniyanandi.sentinel.persistence.AlertRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AlertControllerTest {

    private AlertRepository repository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        repository = mock(AlertRepository.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AlertController(repository))
                .build();
    }

    @Test
    void returnsAlertWhenIdExists() throws Exception {
        AlertEntity alert = new AlertEntity(
                "alert-001",
                "event-001",
                "merchant-42",
                "latency_ms",
                620.0,
                100.0,
                100.0,
                5.2,
                AlertEntity.Severity.HIGH,
                "Latency above baseline",
                Instant.parse("2026-10-23T12:00:00Z")
        );

        when(repository.findById("alert-001"))
                .thenReturn(Optional.of(alert));

        mockMvc.perform(get("/v1/alerts/alert-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertId").value("alert-001"))
                .andExpect(jsonPath("$.entityId").value("merchant-42"))
                .andExpect(jsonPath("$.severity").value("HIGH"));

        verify(repository).findById("alert-001");
    }

    @Test
    void returnsNotFoundWhenIdDoesNotExist() throws Exception {
        when(repository.findById("missing"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/alerts/missing"))
                .andExpect(status().isNotFound());

        verify(repository).findById("missing");
    }
}