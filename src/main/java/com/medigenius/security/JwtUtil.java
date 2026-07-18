package com.medigenius.security;

import com.medigenius.config.MediGeniusProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * NEW COMPONENT - not present in the Python project.
 *
 * The original MediGenius has no user accounts or login; it only tracks an anonymous
 * session id via a cookie/header (see SessionIdService). Your target stack calls for
 * Spring Security + JWT, so this issues a signed JWT that *carries* that same anonymous
 * session id as its subject claim. This is purely additive: existing frontend calls that
 * only send X-Session-ID keep working unchanged (see SecurityConfig - the chat/session/
 * health endpoints remain permit-all by default, matching the Python app's lack of auth).
 * Wire this in if/when you add real user accounts on top of MediGenius.
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final MediGeniusProperties properties;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String sessionId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.getJwt().getExpirationMs());

        return Jwts.builder()
                .subject(sessionId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    public String extractSessionId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
