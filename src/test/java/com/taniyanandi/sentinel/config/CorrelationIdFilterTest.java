package com.taniyanandi.sentinel.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void generatesUuidWhenRequestHeaderIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            String id = MDC.get("correlationId");
            assertNotNull(id);
            assertDoesNotThrow(() -> UUID.fromString(id));
        });

        String responseId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertNotNull(responseId);
        assertDoesNotThrow(() -> UUID.fromString(responseId));
        assertNull(MDC.get("correlationId"), "MDC value must be removed after request processing");
    }

    @Test
    void echoesProvidedCorrelationIdAndMakesItAvailableInMdc() throws ServletException, IOException {
        String suppliedId = "test-correlation-123";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, suppliedId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> idSeenInMdc = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> idSeenInMdc.set(MDC.get("correlationId")));

        assertEquals(suppliedId, response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER));
        assertEquals(suppliedId, idSeenInMdc.get());
        assertNull(MDC.get("correlationId"), "MDC value must be removed after request processing");
    }

    @Test
    void clearsMdcWhenDownstreamFilterThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(ServletException.class, () -> filter.doFilter(request, response, (req, res) -> {
            assertNotNull(MDC.get("correlationId"));
            throw new ServletException("simulated downstream failure");
        }));

        assertNull(MDC.get("correlationId"), "MDC value must be removed even when request processing fails");
    }
}
