package com.fitflow.notif.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTests {
    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void preservesIncomingIdInLoggingContextAndResponse() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        request.addHeader("x-correlation-id", "booking-request-123");
        filter.doFilter(request, response, (req, res) ->
                assertEquals("booking-request-123", MDC.get("correlationId")));
        assertEquals("booking-request-123", response.getHeader("x-correlation-id"));
        assertNull(MDC.get("correlationId"));
    }

    @Test
    void generatesDifferentUuidsForRequestsWithoutId() throws Exception {
        var first = new MockHttpServletResponse();
        var second = new MockHttpServletResponse();
        for (var response : new MockHttpServletResponse[]{first, second}) {
            filter.doFilter(new MockHttpServletRequest(), response, (req, res) -> {
                String id = MDC.get("correlationId");
                assertNotNull(id);
                assertEquals(id, UUID.fromString(id).toString());
                assertEquals(id, response.getHeader("x-correlation-id"));
            });
            assertNull(MDC.get("correlationId"));
        }
        assertNotEquals(first.getHeader("x-correlation-id"), second.getHeader("x-correlation-id"));
    }

    @Test
    void blankHeaderGeneratesIdAndFailureCleansContext() {
        var request = new MockHttpServletRequest();
        request.addHeader("x-correlation-id", " ");
        assertThrows(ServletException.class, () ->
                filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
                    assertNotNull(UUID.fromString(MDC.get("correlationId")));
                    throw new ServletException("Test failure");
                }));
        assertNull(MDC.get("correlationId"));
    }
}
