package com.medigenius.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Centralized exception handling. The Python project relied entirely on FastAPI's default
 * error responses (a single 503 HTTPException in chat.py, plus implicit 422s from Pydantic
 * validation) - this class generalizes that into a consistent JSON error envelope across
 * the whole API, per the "clean architecture" requirement in the target stack.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WorkflowUnavailableException.class)
    public ResponseEntity<ErrorResponseDto> handleWorkflowUnavailable(WorkflowUnavailableException ex) {
        log.error("Workflow unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponseDto.of(ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE.value()));
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleSessionNotFound(SessionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseDto.of(ex.getMessage(), HttpStatus.NOT_FOUND.value()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponseDto.of(message, HttpStatus.UNPROCESSABLE_ENTITY.value()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponseDto.of(ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY.value()));
    }

    /** NEW (Feature 1) - duplicate email on registration. */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleEmailExists(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponseDto.of(ex.getMessage(), HttpStatus.CONFLICT.value()));
    }

    /** NEW (Feature 1) - wrong email/password on login. */
    @ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponseDto> handleInvalidCredentials(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponseDto.of("Invalid email or password", HttpStatus.UNAUTHORIZED.value()));
    }

    /** NEW (Feature 1/16) - missing/expired/invalid JWT, or hitting a protected route unauthenticated. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponseDto.of("Authentication required", HttpStatus.UNAUTHORIZED.value()));
    }

    /** NEW (Feature 1/16) - valid token, but insufficient role/ownership for the resource. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponseDto.of("Access denied", HttpStatus.FORBIDDEN.value()));
    }

    /** NEW (Feature 6) - PDF larger than the configured multipart max-file-size. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDto> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ErrorResponseDto.of("Uploaded file is too large", HttpStatus.PAYLOAD_TOO_LARGE.value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseDto.of("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
}
