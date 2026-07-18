package com.medigenius.dto;

/**
 * Mirrors backend/app/schemas/chat.py -> ChatResponse.
 * Response body of POST /api/v1/chat: {response, source, timestamp, success}
 */
public record ChatResponseDto(
        String response,
        String source,
        String timestamp,
        boolean success
) {
}
