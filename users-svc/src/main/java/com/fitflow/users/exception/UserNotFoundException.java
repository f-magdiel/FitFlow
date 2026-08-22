package com.fitflow.users.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(UUID id) {
        super("No existe un usuario con id: " + id);
    }
}
