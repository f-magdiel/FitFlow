package com.fitflow.notif.service.model;

public record CreateNotificationCommand(
        String userId,
        String type,
        String message
) {
}
