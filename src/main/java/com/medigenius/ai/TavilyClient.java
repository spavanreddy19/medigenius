package com.medigenius.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medigenius.config.MediGeniusProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Replaces backend/app/tools/tavily_search.py -> singleton TavilySearchResults(max_results=3).
 * Calls Tavily's REST API directly (POST https://api.tavily.com/search).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TavilyClient {

    private final RestClient restClient;
    private final MediGeniusProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Equivalent of TavilySearchResults.invoke(query) in agents/tavily.py.
     * Returns up to max-results result "content" snippets.
     */
    public List<String> search(String query) {
        List<String> results = new ArrayList<>();
        String apiKey = properties.getTavily().getApiKey();

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("TAVILY_API_KEY not configured - skipping Tavily fallback for query '{}'.", query);
            return results;
        }

        try {
            Map<String, Object> body = Map.of(
                    "api_key", apiKey,
                    "query", query,
                    "max_results", properties.getTavily().getMaxResults()
            );

            String response = restClient.post()
                    .uri(properties.getTavily().getBaseUrl())
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode resultsNode = objectMapper.readTree(response).path("results");
            for (JsonNode result : resultsNode) {
                String content = result.path("content").asText(null);
                if (content != null && !content.isBlank()) {
                    results.add(content);
                }
            }
        } catch (Exception e) {
            log.warn("Tavily search failed for query '{}': {}", query, e.getMessage());
        }
        return results;
    }
}
