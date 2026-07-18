package com.medigenius.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Equivalent of the CORSMiddleware block in backend/app/main.py:
 *
 *   app.add_middleware(
 *       CORSMiddleware,
 *       allow_origins=["*"],
 *       allow_credentials=True,
 *       allow_methods=["*"],
 *       allow_headers=["*"],
 *   )
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig {

    private final MediGeniusProperties properties;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        String origins = properties.getCors().getAllowedOrigins();

        if ("*".equals(origins)) {
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            config.setAllowedOrigins(Arrays.asList(origins.split(",")));
        }
        config.setAllowCredentials(true);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-Session-ID", "X-Session-Token"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    /**
     * Shared RestClient used by WikipediaClient and TavilyClient (ai package)
     * to call their respective REST APIs, replacing Python's `requests`/langchain wrappers.
     */
    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }
}
