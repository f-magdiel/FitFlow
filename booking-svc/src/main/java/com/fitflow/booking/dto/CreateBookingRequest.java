package com.fitflow.booking.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateBookingRequest(
        @NotNull(message = "userId es requerido")
        UUID userId,

        @NotNull(message = "classId es requerido")
        UUID classId
) {
}
