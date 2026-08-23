package com.fitflow.booking.dto.client;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String fullName,
        String email,
        Instant createdAt
) {
}
