package com.fitflow.notif.api.dto;

import com.fitflow.notif.service.model.Notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String userId,
        String type,
        String message,
        String status,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.id(),
                notification.userId(),
                notification.type(),
                notification.message(),
                notification.status(),
                notification.createdAt()
        );
    }
}
