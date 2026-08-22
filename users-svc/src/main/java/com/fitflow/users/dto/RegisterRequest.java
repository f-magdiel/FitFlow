package com.fitflow.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "fullName es requerido")
        @Size(max = 120)
        String fullName,

        @NotBlank(message = "email es requerido")
        @Email(message = "email debe tener un formato valido")
        @Size(max = 180)
        String email,

        @NotBlank(message = "password es requerido")
        @Size(min = 8, message = "password debe tener al menos 8 caracteres")
        String password
) {
}
