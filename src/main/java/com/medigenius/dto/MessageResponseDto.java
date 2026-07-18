package com.medigenius.dto;

/**
 * Mirrors backend/app/schemas/session.py -> MessageResponse.
 * Used inside GET /api/v1/history and GET /api/v1/session/{id} -> {messages: [...]}
 */
public record MessageResponseDto(
        String role,
        String content,
        String source,
        String timestamp
) {
}
