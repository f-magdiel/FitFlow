package com.fitflow.notif.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNotificationRequest(
        @NotBlank @Size(max = 100) String userId,
        @NotBlank @Size(max = 50) String type,
        @NotBlank @Size(max = 1000) String message
) {
}
