package com.medigenius.exception;

/** NEW EXCEPTION (Feature 1) - thrown by AuthenticationService.register() on duplicate email. */
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("An account with email '" + email + "' already exists");
    }
}
