package com.medigenius.exception;

/** NEW EXCEPTION (Feature 1) - thrown by AuthenticationService.login() on bad email/password. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
