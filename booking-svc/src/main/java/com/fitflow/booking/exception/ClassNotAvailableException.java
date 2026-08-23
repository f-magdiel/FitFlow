package com.fitflow.booking.exception;

import java.util.UUID;

public class ClassNotAvailableException extends RuntimeException {

    public ClassNotAvailableException(UUID id) {
        super("La clase " + id + " no esta disponible para reservar");
    }
}
