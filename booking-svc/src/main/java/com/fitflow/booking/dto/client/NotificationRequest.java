package com.fitflow.booking.dto.client;

public record NotificationRequest(
        String userId,
        String type,
        String message
) {
}
