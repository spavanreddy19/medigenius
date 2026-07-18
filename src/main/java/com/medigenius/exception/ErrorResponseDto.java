package com.medigenius.exception;

import java.time.LocalDateTime;

/**
 * Standard error envelope returned by GlobalExceptionHandler.
 * Shape: {"success": false, "error": "...", "timestamp": "...", "status": 500}
 */
public record ErrorResponseDto(
        boolean success,
        String error,
        String timestamp,
        int status
) {
    public static ErrorResponseDto of(String error, int status) {
        return new ErrorResponseDto(false, error, LocalDateTime.now().toString(), status);
    }
}
