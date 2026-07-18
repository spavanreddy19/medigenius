package com.medigenius.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security chain. IMPORTANT BEHAVIORAL NOTE:
 *
 * The Python/FastAPI original has NO authentication or authorization on any endpoint -
 * every route under /api/v1/* is open to anyone who can reach the server. To keep API
 * behavior identical (per the "Keep API behavior exactly the same" instruction), every
 * existing endpoint remains permit-all. The JwtUtil/JwtAuthFilter above are wired in but are
 * opt-in and additive: they populate an authenticated principal ONLY if a valid token is
 * presented, without ever rejecting unauthenticated requests.
 *
 * MODIFIED (Feature 1 - JWT Authentication): added UserJwtAuthFilter (real-user Bearer JWT),
 * a BCryptPasswordEncoder bean, an AuthenticationManager bean (used by AuthenticationService
 * for login), and route rules that require a valid user JWT for the new
 * /api/users/**, /api/pdf/**, and /api/conversations/** endpoints, while explicitly keeping
 * /api/auth/** and every pre-existing /api/v1/** endpoint public exactly as before.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserJwtAuthFilter userJwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Stateless JSON API, mirrors FastAPI's lack of CSRF protection
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        // Existing, unauthenticated API surface - unchanged.
                        .requestMatchers("/api/v1/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/chat/public").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // NEW (Feature 1) - everything else requires a valid user JWT.
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(userJwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** NEW (Feature 1) - used by AuthenticationService to hash/verify user passwords. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** NEW (Feature 1) - used by AuthenticationService.login() to verify email/password pairs. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /** NEW (Feature 1) - wires CustomUserDetailsService + BCrypt into Spring Security's DAO provider. */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            CustomUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
}
