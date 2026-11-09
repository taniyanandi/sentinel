package com.taniyanandi.sentinel.api;

import com.taniyanandi.sentinel.service.EventIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
class EventControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventIngestionService ingestionService;

    @Test
    void rejectsMissingRequiredFieldsBeforeIngestion() throws Exception {
        mockMvc.perform(post("/v1/events")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ingestionService);
    }

    @Test
    void rejectsMissingValueBeforeIngestion() throws Exception {
        mockMvc.perform(post("/v1/events")
                        .contentType("application/json")
                        .content("""
                                {"eventId":"e1","source":"test","entityId":"entity-1",
                                 "metric":"temperature","timestamp":"2026-11-10T00:00:00Z"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ingestionService);
    }

    @Test
    void rejectsNegativeValueBeforeIngestion() throws Exception {
        mockMvc.perform(post("/v1/events")
                        .contentType("application/json")
                        .content("""
                                {"eventId":"e1","source":"test","entityId":"entity-1",
                                 "metric":"temperature","value":-1,
                                 "timestamp":"2026-11-10T00:00:00Z"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ingestionService);
    }

    @Test
    void acceptsValidRequestAndInvokesIngestion() throws Exception {
        mockMvc.perform(post("/v1/events")
                        .contentType("application/json")
                        .content("""
                                {"eventId":"e1","source":"test","entityId":"entity-1",
                                 "metric":"temperature","value":0,
                                 "timestamp":"2026-11-10T00:00:00Z"}
                                """))
                .andExpect(status().isAccepted());

        verify(ingestionService).publish(org.mockito.ArgumentMatchers.any());
    }
}
