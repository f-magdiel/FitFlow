package com.fitflow.booking.dto;

import com.fitflow.booking.entity.Booking;

import java.time.Instant;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID userId,
        UUID classId,
        String className,
        String status,
        Instant startTime,
        Instant endTime,
        Instant createdAt,
        Instant cancelledAt
) {
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getUserId(),
                booking.getFitnessClass().getId(),
                booking.getFitnessClass().getName(),
                booking.getStatus().name(),
                booking.getFitnessClass().getStartTime(),
                booking.getFitnessClass().getEndTime(),
                booking.getCreatedAt(),
                booking.getCancelledAt()
        );
    }
}
