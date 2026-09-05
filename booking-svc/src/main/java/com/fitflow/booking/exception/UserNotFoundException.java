package com.fitflow.booking.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID id) {
        super("No existe el usuario " + id);
    }
}
