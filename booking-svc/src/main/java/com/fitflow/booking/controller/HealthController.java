package com.fitflow.booking.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HealthController {

    private static final int DB_CHECK_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;

    @GetMapping("/healthz")
    public Map<String, String> healthz() {
        return Map.of("status", "ok");
    }

    @GetMapping("/readyz")
    public ResponseEntity<Map<String, String>> readyz() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(DB_CHECK_TIMEOUT_SECONDS)) {
                return ResponseEntity.ok(Map.of("status", "ok"));
            }
        } catch (Exception e) {
            log.warn("readyz: fallo la verificacion de conexion a la base de datos", e);
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "unavailable"));
    }
}
