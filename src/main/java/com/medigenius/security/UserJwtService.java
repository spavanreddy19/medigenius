package com.medigenius.security;

import com.medigenius.config.MediGeniusProperties;
import com.medigenius.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * NEW COMPONENT (Feature 1/3 - JWT Authentication).
 *
 * Issues/validates "Authorization: Bearer <token>" JWTs for real, logged-in users.
 * Deliberately separate from the existing {@link JwtUtil} (which signs anonymous
 * X-Session-Token JWTs for SessionIdService and is left completely untouched) so the two
 * token types can never be confused with one another, and so this can be locked down or
 * rotated independently.
 */
@Component
@RequiredArgsConstructor
public class UserJwtService {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_ROLE = "role";

    private final MediGeniusProperties properties;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(properties.getAuth().getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.getAuth().getJwtExpirationMs());

        return Jwts.builder()
                .subject(user.getEmail())
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_ROLE, user.getRole())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return parseClaims(token).get(CLAIM_USER_ID, Long.class);
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
