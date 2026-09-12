package com.fitflow.booking.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fitflow.booking.client.NotificationClient;
import com.fitflow.booking.dto.BookingResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTimeoutTests {

    @Test
    void slowNotificationLogsErrorAndReturnsWithoutThrowing() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        CountDownLatch releaseResponse = new CountDownLatch(1);
        server.createContext("/notifications", exchange -> {
            try {
                releaseResponse.await();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        Logger logger = (Logger) LoggerFactory.getLogger(NotificationClient.class);
        ListAppender<ILoggingEvent> logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
        server.start();
        try {
            var restClient = new RestClientConfig().notifRestClient(RestClient.builder(),
                    "http://127.0.0.1:" + server.getAddress().getPort());
            var client = new NotificationClient(restClient);
            var booking = new BookingResponse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    "Yoga", "CONFIRMED", null, null, null, null);

            long start = System.nanoTime();
            assertTimeoutPreemptively(Duration.ofSeconds(15),
                    () -> assertDoesNotThrow(() -> client.sendBookingConfirmed(booking)));
            assertTrue(Duration.ofNanos(System.nanoTime() - start).toMillis() >= 11000);
            assertEquals(4, logs.list.stream().filter(event -> event.getLevel() == Level.ERROR).count());
            assertTrue(logs.list.stream().anyMatch(event -> event.getLevel() == Level.ERROR
                    && event.getFormattedMessage().contains("BOOKING_CONFIRMED")
                    && event.getThrowableProxy() != null));
        } finally {
            releaseResponse.countDown();
            server.stop(0);
            logger.detachAppender(logs);
            logs.stop();
        }
    }
}
