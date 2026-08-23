package com.fitflow.booking.exception;

public class DuplicateBookingException extends RuntimeException {

    public DuplicateBookingException() {
        super("El usuario ya tiene una reserva confirmada para esta clase");
    }
}
