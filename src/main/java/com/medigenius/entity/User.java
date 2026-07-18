package com.medigenius.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * NEW ENTITY (Feature 2 - User Entity).
 *
 * Real, persisted user accounts. This is additive: it does not replace the existing
 * anonymous X-Session-ID / X-Session-Token flow (SessionIdService / JwtUtil), which keeps
 * working unchanged for unauthenticated visitors. When a request DOES carry a valid
 * "Authorization: Bearer <token>" issued by UserJwtService, the resolved User below gets
 * associated with that same session's Conversation/Message rows (see ChatService).
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /** BCrypt hash - never the raw password. */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "role", nullable = false, length = 30)
    @Builder.Default
    private String role = "USER";

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
