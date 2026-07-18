package com.medigenius.dto;

/**
 * Mirrors backend/app/schemas/session.py -> SessionResponse.
 * Used inside GET /api/v1/sessions -> {sessions: [SessionResponseDto, ...]}
 */
public record SessionResponseDto(
        String sessionId,
        String preview,
        String lastActive
) {
}
