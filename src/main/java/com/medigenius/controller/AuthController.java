package com.medigenius.controller;

import com.medigenius.dto.AuthResponseDto;
import com.medigenius.dto.LoginRequestDto;
import com.medigenius.dto.RegisterRequestDto;
import com.medigenius.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * NEW CONTROLLER (Feature 1 - JWT Authentication).
 * Public endpoints (see SecurityConfig: /api/auth/** is permitAll). Everything else in
 * the app is untouched/still reachable exactly as before.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    /** POST /api/auth/register - {name, email, password} -> {token, tokenType, user}. */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authenticationService.register(request));
    }

    /** POST /api/auth/login - {email, password} -> {token, tokenType, user}. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }
}
