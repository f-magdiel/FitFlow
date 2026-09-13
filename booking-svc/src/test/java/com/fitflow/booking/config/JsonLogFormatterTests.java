package com.fitflow.booking.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.junit.jupiter.api.Test;
import org.slf4j.event.KeyValuePair;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonLogFormatterTests {
    @Test
    void emitsParseableSingleLineJsonWithRequiredFieldsAndEscapedException() {
        var event = new LoggingEvent();
        event.setLevel(Level.ERROR);
        event.setTimeStamp(0);
        event.setLoggerName("test");
        event.setThreadName("main");
        event.setMessage("Message with \"quotes\"\nand newline");
        event.setMDCPropertyMap(Map.of("correlationId", "test-123"));
        event.addKeyValuePair(new KeyValuePair("event", "notification.failed"));
        event.addKeyValuePair(new KeyValuePair("retry", 2));
        event.setThrowableProxy(new ThrowableProxy(new IllegalStateException("Test failure")));
        String output = new JsonLogFormatter().format(event);
        assertEquals(1, output.lines().count());
        var json = new ObjectMapper().readTree(output);
        assertEquals("test-123", json.get("correlation_id").asText());
        assertEquals("booking-svc", json.get("service").asText());
        assertEquals("notification.failed", json.get("event").asText());
        assertEquals("ERROR", json.get("level").asText());
        assertEquals("1970-01-01T00:00:00Z", json.get("timestamp").asText());
        assertEquals(2, json.get("retry").asInt());
        assertTrue(json.get("exception").asText().contains("Test failure"));
        assertEquals(event.getMessage(), json.get("message").asText());
    }

    @Test
    void startupLogsHaveNullCorrelationIdAndDefaultEvent() {
        var event = new LoggingEvent();
        event.setLevel(Level.INFO);
        event.setMDCPropertyMap(Map.of());
        event.setMessage("Starting service");
        var json = new ObjectMapper().readTree(new JsonLogFormatter().format(event));
        assertTrue(json.has("correlation_id"));
        assertTrue(json.get("correlation_id").isNull());
        assertEquals("application.log", json.get("event").asText());
    }
}
