package com.fitflow.booking.exception;

import java.util.UUID;

public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(UUID id) {
        super("No existe la reserva " + id);
    }
}
