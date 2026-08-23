package com.fitflow.users.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Email o password invalidos");
    }
}
