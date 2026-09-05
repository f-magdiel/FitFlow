package com.fitflow.booking.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        int status,
        String error,
        List<String> details,
        Instant timestamp
) {
    public static ErrorResponse of(int status, String error, List<String> details) {
        return new ErrorResponse(status, error, details, Instant.now());
    }
}
