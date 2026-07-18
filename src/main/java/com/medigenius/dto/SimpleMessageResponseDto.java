package com.medigenius.dto;

/**
 * Generic {message, success} response, used by:
 *   POST   /api/v1/clear     -> {"message": "Conversation cleared", "success": true}
 *   DELETE /api/v1/session/{id} -> {"message": "Session deleted", "success": true}
 */
public record SimpleMessageResponseDto(
        String message,
        boolean success
) {
}
