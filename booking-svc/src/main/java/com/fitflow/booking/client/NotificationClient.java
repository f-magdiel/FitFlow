package com.fitflow.booking.client;

import com.fitflow.booking.dto.BookingResponse;
import com.fitflow.booking.dto.client.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class NotificationClient {

    private final RestClient restClient;

    public NotificationClient(@Qualifier("notifRestClient") RestClient restClient) {
        this.restClient = restClient;
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
        try {
            restClient.post()
                    .uri("/notifications")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("No se pudo enviar notificacion type={} userId={}", request.type(), request.userId(), ex);
        }
    }
}
