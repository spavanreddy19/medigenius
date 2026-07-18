package com.medigenius.controller;

import com.medigenius.dto.HealthResponseDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Direct port of backend/app/api/v1/endpoints/health.py.
 * Response body preserved verbatim: {"status": "healthy", "service": "MediGenius Backend v2"}
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public HealthResponseDto health() {
        return new HealthResponseDto("healthy", "MediGenius Backend v2");
    }
}
