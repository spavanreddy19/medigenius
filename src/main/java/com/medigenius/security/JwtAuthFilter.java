package com.medigenius.security;

import com.medigenius.config.MediGeniusProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * NEW COMPONENT (see JwtUtil javadoc for why this exists).
 *
 * If the incoming request carries a valid X-Session-Token JWT, this authenticates the
 * request as that session id (anonymous "user" = ROLE_SESSION principal). This never
 * blocks requests without a token - all MediGenius endpoints are public by design
 * (matching the Python app, which has no auth at all), so this filter is additive-only
 * and safe to leave in place.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final MediGeniusProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = request.getHeader(properties.getJwt().getHeaderName());

        if (token != null && !token.isBlank() && jwtUtil.isValid(token)) {
            String sessionId = jwtUtil.extractSessionId(token);
            var authentication = new UsernamePasswordAuthenticationToken(
                    sessionId, null, List.of(() -> "ROLE_SESSION"));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
