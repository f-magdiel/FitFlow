package com.fitflow.booking.exception;

import java.util.UUID;

public class ClassNotFoundException extends RuntimeException {

    public ClassNotFoundException(UUID id) {
        super("No existe la clase " + id);
    }
}
