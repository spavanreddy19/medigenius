package com.medigenius.dto;

import java.util.List;

/**
 * Response of GET /api/v1/sessions -> {"sessions": [...], "success": true}
 */
public record SessionsListResponseDto(
        List<SessionResponseDto> sessions,
        boolean success
) {
}
