package com.medigenius.dto;

/**
 * Response of GET /api/v1/health -> {"status": "ok", "service": "MediGenius"}
 */
public record HealthResponseDto(
        String status,
        String service
) {
}
