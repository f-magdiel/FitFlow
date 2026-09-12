package com.fitflow.booking.client;

import com.fitflow.booking.dto.BookingResponse;
import org.junit.jupiter.api.Test;
import com.fitflow.booking.config.CorrelationIdFilter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class NotificationRetryTests {
    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://notif-svc");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final List<Long> delays = new ArrayList<>();

    @Test
    void cancellationPreservesIncomingCorrelationIdAcrossAllRetries() throws Exception {
        verifyCancellationCorrelationId("cancel-request-123");
    }

    @Test
    void cancellationGeneratesAndPropagatesCorrelationIdWhenMissing() throws Exception {
        verifyCancellationCorrelationId(null);
    }

    private void verifyCancellationCorrelationId(String incomingId) throws Exception {
        var request = new MockHttpServletRequest("DELETE", "/api/bookings/" + booking().id());
        var response = new MockHttpServletResponse();
        if (incomingId != null) {
            request.addHeader("x-correlation-id", incomingId);
        }
        new CorrelationIdFilter().doFilter(request, response, (req, res) -> {
            String correlationId = response.getHeader("x-correlation-id");
            assertNotNull(correlationId);
            if (incomingId == null) {
                assertNotNull(UUID.fromString(correlationId));
            } else {
                assertEquals(incomingId, correlationId);
            }
            for (int attempt = 0; attempt < 4; attempt++) {
                server.expect(requestTo("http://notif-svc/notifications"))
                        .andExpect(header("x-correlation-id", correlationId))
                        .andExpect(jsonPath("$.type").value("BOOKING_CANCELLED"))
                        .andRespond(withStatus(attempt == 3
                                ? HttpStatus.CREATED : HttpStatus.SERVICE_UNAVAILABLE));
            }
            new NotificationClient(builder.build(), delays::add).sendBookingCancelled(booking());
        });
        server.verify();
        assertEquals(3, delays.size());
        assertNull(org.slf4j.MDC.get("correlationId"));
    }

    @Test
    void propagatesSameCorrelationIdOnInitialCallAndRetry() throws Exception {
        for (var status : new HttpStatus[]{HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.CREATED}) {
            server.expect(requestTo("http://notif-svc/notifications"))
                    .andExpect(header("x-correlation-id", "booking-request-123"))
                    .andRespond(withStatus(status));
        }
        var request = new MockHttpServletRequest();
        request.addHeader("x-correlation-id", "booking-request-123");
        new CorrelationIdFilter().doFilter(request, new MockHttpServletResponse(),
                (req, res) -> new NotificationClient(builder.build(), delays::add)
                        .sendBookingConfirmed(booking()));
        server.verify();
        assertEquals(1, delays.size());
    }

    @Test
    void retriesThreeTimesWithExponentialBackoffAndBoundedJitter() {
        for (int i = 0; i < 4; i++) {
            expect(HttpStatus.SERVICE_UNAVAILABLE);
        }
        var client = new NotificationClient(builder.build(), delays::add);
        assertDoesNotThrow(() -> client.sendBookingConfirmed(booking()));
        server.verify();
        assertEquals(3, delays.size());
        for (int i = 0; i < delays.size(); i++) {
            long base = 500L << i;
            assertTrue(delays.get(i) >= base && delays.get(i) <= base + base / 4);
        }
    }

    @Test
    void stopsRetryingOnceCancellationNotificationSucceeds() {
        expect(HttpStatus.SERVICE_UNAVAILABLE);
        expect(HttpStatus.CREATED);
        new NotificationClient(builder.build(), delays::add).sendBookingCancelled(booking());
        server.verify();
        assertEquals(1, delays.size());
    }

    @Test
    void successfulFirstAttemptDoesNotWait() {
        expect(HttpStatus.CREATED);
        new NotificationClient(builder.build(), delays::add).sendBookingConfirmed(booking());
        server.verify();
        assertTrue(delays.isEmpty());
    }

    @Test
    void interruptionStopsRetriesAndPreservesInterruptFlag() {
        expect(HttpStatus.SERVICE_UNAVAILABLE);
        var client = new NotificationClient(builder.build(), milliseconds -> {
            throw new InterruptedException("Test interruption");
        });
        try {
            client.sendBookingConfirmed(booking());
            assertTrue(Thread.currentThread().isInterrupted());
            server.verify();
        } finally {
            Thread.interrupted();
        }
    }

    private void expect(HttpStatus status) {
        server.expect(requestTo("http://notif-svc/notifications")).andRespond(withStatus(status));
    }

    private BookingResponse booking() {
        return new BookingResponse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Yoga", "CONFIRMED", null, null, null, null);
    }
}
