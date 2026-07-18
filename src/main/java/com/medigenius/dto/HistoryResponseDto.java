package com.medigenius.dto;

import java.util.List;

/**
 * Response of GET /api/v1/history -> {"messages": [...], "success": true}
 */
public record HistoryResponseDto(
        List<MessageResponseDto> messages,
        boolean success
) {
}
