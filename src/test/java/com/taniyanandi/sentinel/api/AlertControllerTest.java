package com.taniyanandi.sentinel.api;

import com.taniyanandi.sentinel.persistence.AlertEntity;
import com.taniyanandi.sentinel.persistence.AlertRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertController.class)
public class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    AlertRepository alertRepository;

    @Test
    void returnsAlertWhenFound() throws Exception {
        AlertEntity alert = new AlertEntity(
                "alert-123",
                "event-456",
                "merchant-42",
                "latency_ms",
                160.0,
                100.0,
                10.0,
                6.0,
                AlertEntity.Severity.CRITICAL,
                "Value deviated 6.00 standard deviations from baseline",
                Instant.parse("2026-10-22T12:00:00Z")
        );

        when(alertRepository.findById("alert-123"))
                .thenReturn(Optional.of(alert));

        mockMvc.perform(get("/v1/alerts/alert-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertId").value("alert-123"))
                .andExpect(jsonPath("$.entityId").value("merchant-42"))
                .andExpect(jsonPath("$.severity").value(AlertEntity.Severity.CRITICAL.name()));
    }

    @Test
    void returnsNotFoundWhenAlertDoesNotExist() throws Exception {
        when(alertRepository.findById("missing-alert")).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/alerts/missing-alert"))
                .andExpect(status().isNotFound());

        verify(alertRepository).findById("missing-alert");
    }
}
