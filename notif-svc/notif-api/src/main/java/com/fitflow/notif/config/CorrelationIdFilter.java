package com.fitflow.notif.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(CorrelationIdFilter.class);
    public static final String HEADER = "x-correlation-id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        String previous = MDC.get(MDC_KEY);
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        long started = System.nanoTime();
        boolean completed = false;
        try {
            filterChain.doFilter(request, response);
            completed = true;
        } finally {
            long durationMs = (System.nanoTime() - started) / 1_000_000;
            if (completed) {
                LOG.atInfo().addKeyValue("event", "http.request.completed")
                        .addKeyValue("method", request.getMethod()).addKeyValue("path", request.getRequestURI())
                        .addKeyValue("status", response.getStatus()).addKeyValue("duration_ms", durationMs).log("HTTP request completed method={} path={} status={} durationMs={}",
                        request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
            } else {
                LOG.atError().addKeyValue("event", "http.request.failed")
                        .addKeyValue("method", request.getMethod()).addKeyValue("path", request.getRequestURI())
                        .addKeyValue("duration_ms", durationMs).log("HTTP request failed method={} path={} durationMs={}",
                        request.getMethod(), request.getRequestURI(), durationMs);
            }
            if (previous == null) {
                MDC.remove(MDC_KEY);
            } else {
                MDC.put(MDC_KEY, previous);
            }
        }
    }
}
