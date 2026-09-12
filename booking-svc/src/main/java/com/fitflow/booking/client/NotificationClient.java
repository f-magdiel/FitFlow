package com.fitflow.booking.client;

import com.fitflow.booking.dto.BookingResponse;
import com.fitflow.booking.dto.client.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import com.fitflow.booking.config.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class NotificationClient {

    private final RestClient restClient;
    private final Sleeper sleeper;

    @Autowired
    public NotificationClient(@Qualifier("notifRestClient") RestClient restClient) {
        this(restClient, Thread::sleep);
    }

    NotificationClient(RestClient restClient, Sleeper sleeper) {
        this.restClient = restClient;
        this.sleeper = sleeper;
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long milliseconds) throws InterruptedException;
    }

    public void sendBookingConfirmed(BookingResponse booking) {
        send(new NotificationRequest(
                booking.userId().toString(),
                "BOOKING_CONFIRMED",
                "Tu reserva para " + booking.className() + " ha sido confirmada."
        ));
    }

    public void sendBookingCancelled(BookingResponse booking) {
        send(new NotificationRequest(
                booking.userId().toString(),
                "BOOKING_CANCELLED",
                "Tu reserva para " + booking.className() + " ha sido cancelada."
        ));
    }

    private void send(NotificationRequest request) {
        for (int attempt = 1; attempt <= 4; attempt++) {
            try {
                restClient.post()
                        .uri("/notifications")
                        .headers(headers -> {
                            String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
                            if (correlationId != null) {
                                headers.set(CorrelationIdFilter.HEADER, correlationId);
                            }
                        })
                        .body(request)
                        .retrieve()
                        .toBodilessEntity();
                return;
            } catch (RestClientException ex) {
                if (attempt == 1) {
                    log.atError().addKeyValue("event", "notification.failed").addKeyValue("attempt", attempt).log("Fallo el intento inicial de notificacion a notif-svc (timeout: 2 s) type={} userId={}",
                            request.type(), request.userId(), ex);
                } else {
                    log.atError().addKeyValue("event", "notification.failed").addKeyValue("retry", attempt - 1).addKeyValue("exhausted", attempt == 4).log("Fallo notificacion a notif-svc reintento={}/3 agotados={} (timeout: 2 s) type={} userId={}",
                            attempt - 1, attempt == 4, request.type(), request.userId(), ex);
                }
                if (attempt == 4) {
                    return;
                }
                // Backoff de 500, 1000 y 2000 ms mas jitter de hasta el 25%.
                long baseDelay = 500L << (attempt - 1);
                long delay = baseDelay + ThreadLocalRandom.current().nextLong(baseDelay / 4 + 1);
                log.atInfo().addKeyValue("event", "notification.retry").addKeyValue("retry", attempt).addKeyValue("delay_ms", delay).log("Reintentando notificacion a notif-svc reintento={}/3 esperaMs={} type={} userId={}",
                        attempt, delay, request.type(), request.userId());
                try {
                    sleeper.sleep(delay);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    log.atError().addKeyValue("event", "notification.interrupted").log("Reintentos de notificacion interrumpidos type={} userId={}",
                            request.type(), request.userId(), interrupted);
                    return;
                }
            }
        }
    }
}
