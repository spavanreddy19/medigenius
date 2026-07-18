package com.medigenius.dto;

/**
 * Response of POST /api/v1/new-chat -> {"message", "session_id", "success"}
 */
public record NewChatResponseDto(
        String message,
        String sessionId,
        boolean success
) {
}
