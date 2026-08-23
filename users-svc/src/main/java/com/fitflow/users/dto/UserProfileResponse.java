package com.fitflow.users.dto;

import com.fitflow.users.entity.User;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String fullName,
        String email,
        Instant createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(user.getId(), user.getFullName(), user.getEmail(), user.getCreatedAt());
    }
}
