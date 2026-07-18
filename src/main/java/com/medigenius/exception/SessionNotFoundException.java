package com.medigenius.exception;

/**
 * Thrown when a requested session id has no associated data. The Python endpoints don't
 * actually raise this (they return empty results for unknown session ids), so this is
 * reserved for future stricter validation - not currently thrown by ported code, kept
 * here to complete the exception hierarchy requested in the target architecture.
 */
public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException(String sessionId) {
        super("Session not found: " + sessionId);
    }
}
