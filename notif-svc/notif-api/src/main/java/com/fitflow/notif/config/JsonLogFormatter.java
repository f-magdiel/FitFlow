package com.fitflow.notif.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLogFormatter;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** One JSON object per line, including logs emitted outside an HTTP request. */
public class JsonLogFormatter implements StructuredLogFormatter<ILoggingEvent> {
    private final JsonWriter<Map<String, Object>> writer = JsonWriter.<Map<String, Object>>standard()
            .withNewLineAtEnd();

    @Override
    public String format(ILoggingEvent event) {
        Map<String, Object> fields = new LinkedHashMap<>();
        if (event.getKeyValuePairs() != null) {
            event.getKeyValuePairs().forEach(pair -> fields.put(pair.key, pair.value));
        }
        fields.put("timestamp", Instant.ofEpochMilli(event.getTimeStamp()).toString());
        fields.put("level", event.getLevel().toString());
        fields.put("service", "notif-svc");
        fields.put("correlation_id", event.getMDCPropertyMap().get("correlationId"));
        fields.putIfAbsent("event", "application.log");
        fields.put("message", event.getFormattedMessage());
        fields.put("logger", event.getLoggerName());
        fields.put("thread", event.getThreadName());
        if (event.getThrowableProxy() != null) {
            fields.put("exception", ThrowableProxyUtil.asString(event.getThrowableProxy()));
        }
        return writer.writeToString(fields);
    }
}
