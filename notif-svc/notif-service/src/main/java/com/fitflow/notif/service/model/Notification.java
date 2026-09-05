package com.fitflow.notif.service.model;

import java.time.Instant;
import java.util.UUID;

public record Notification(
        UUID id,
        String userId,
        String type,
        String message,
        String status,
        Instant createdAt
) {
}
