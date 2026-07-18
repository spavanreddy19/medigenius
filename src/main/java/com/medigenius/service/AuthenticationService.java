package com.medigenius.service;

import com.medigenius.dto.AuthResponseDto;
import com.medigenius.dto.LoginRequestDto;
import com.medigenius.dto.RegisterRequestDto;
import com.medigenius.entity.User;
import com.medigenius.exception.EmailAlreadyExistsException;
import com.medigenius.exception.InvalidCredentialsException;
import com.medigenius.repository.UserRepository;
import com.medigenius.security.UserJwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * NEW SERVICE (Features 1 & 3 - JWT Authentication).
 * Owns registration and login business logic: password hashing (BCrypt), duplicate-email
 * checks, credential verification (delegated to Spring Security's AuthenticationManager),
 * and JWT issuance via {@link UserJwtService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserJwtService userJwtService;

    /** POST /api/auth/register. */
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.password()))
                .role("USER")
                .build();

        user = userRepository.save(user);
        log.info("Registered new user: {}", normalizedEmail);

        String token = userJwtService.generateToken(user);
        return AuthResponseDto.of(token, userService.toResponseDto(user));
    }

    /** POST /api/auth/login. */
    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password()));
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        String token = userJwtService.generateToken(user);
        return AuthResponseDto.of(token, userService.toResponseDto(user));
    }
}
