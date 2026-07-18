package com.medigenius.dto;

import java.util.List;

/**
 * Response of GET /api/v1/session/{id} -> {"messages": [...], "session_id": "...", "success": true}
 */
public record SessionMessagesResponseDto(
        List<MessageResponseDto> messages,
        String sessionId,
        boolean success
) {
}
