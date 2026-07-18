package com.medigenius.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * NEW COMPONENT (Feature 1/3 - JWT Authentication).
 *
 * Reads a standard "Authorization: Bearer <token>" header, and if it carries a valid
 * {@link UserJwtService} token, populates the SecurityContext with the matching
 * {@link UserPrincipal}. Runs alongside (not instead of) the pre-existing
 * {@link JwtAuthFilter}, which continues to handle the anonymous X-Session-Token flow
 * untouched. Like that filter, this one never rejects requests itself - it only
 * authenticates when a valid token is present; SecurityConfig decides what's protected.
 */
@Component
@RequiredArgsConstructor
public class UserJwtAuthFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final UserJwtService userJwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(AUTH_HEADER);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());

            if (userJwtService.isValid(token)) {
                try {
                    String email = userJwtService.extractEmail(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    var authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (Exception ignored) {
                    // Invalid/stale token referencing a deleted user - fall through unauthenticated.
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
