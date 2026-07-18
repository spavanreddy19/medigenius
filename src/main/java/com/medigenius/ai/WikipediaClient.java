package com.medigenius.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medigenius.config.MediGeniusProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Replaces backend/app/tools/wikipedia_search.py -> singleton WikipediaAPIWrapper
 * (top_k_results=2, doc_content_chars_max=2000).
 *
 * LangChain4j has no built-in Wikipedia tool, so this is a direct call to Wikipedia's
 * public search + extract REST API, reproducing the same top-k / char-limit behavior.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WikipediaClient {

    private final RestClient restClient;
    private final MediGeniusProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Equivalent of WikipediaAPIWrapper.run(query) in agents/wikipedia.py.
     * Returns up to top-k page extracts, each truncated to maxDocChars.
     */
    public List<String> search(String query) {
        List<String> results = new ArrayList<>();
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String searchUrl = properties.getWikipedia().getBaseUrl()
                    + "?action=query&list=search&srsearch=" + encodedQuery
                    + "&format=json&srlimit=" + properties.getWikipedia().getTopKResults();

            String searchResponse = restClient.get().uri(searchUrl).retrieve().body(String.class);
            JsonNode searchResults = objectMapper.readTree(searchResponse)
                    .path("query").path("search");

            for (JsonNode page : searchResults) {
                String title = page.path("title").asText();
                String extract = fetchExtract(title);
                if (extract != null && !extract.isBlank()) {
                    results.add(extract);
                }
            }
        } catch (Exception e) {
            log.warn("Wikipedia search failed for query '{}': {}", query, e.getMessage());
        }
        return results;
    }

    private String fetchExtract(String title) {
        try {
            String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
            String extractUrl = properties.getWikipedia().getBaseUrl()
                    + "?action=query&prop=extracts&exintro=true&explaintext=true"
                    + "&titles=" + encodedTitle + "&format=json";

            String response = restClient.get().uri(extractUrl).retrieve().body(String.class);
            JsonNode pages = objectMapper.readTree(response).path("query").path("pages");

            String extract = null;
            var fields = pages.fields();
            if (fields.hasNext()) {
                extract = fields.next().getValue().path("extract").asText(null);
            }
            if (extract == null) {
                return null;
            }
            int maxChars = properties.getWikipedia().getMaxDocChars();
            return extract.length() > maxChars ? extract.substring(0, maxChars) : extract;
        } catch (Exception e) {
            log.debug("Failed to fetch extract for '{}': {}", title, e.getMessage());
            return null;
        }
    }
}
