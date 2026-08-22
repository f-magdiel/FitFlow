package com.fitflow.users.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "email es requerido")
        String email,

        @NotBlank(message = "password es requerido")
        String password
) {
}
