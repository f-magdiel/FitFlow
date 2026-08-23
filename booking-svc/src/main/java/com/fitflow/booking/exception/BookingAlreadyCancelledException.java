package com.fitflow.booking.exception;

import java.util.UUID;

public class BookingAlreadyCancelledException extends RuntimeException {

    public BookingAlreadyCancelledException(UUID id) {
        super("La reserva " + id + " ya fue cancelada");
    }
}
