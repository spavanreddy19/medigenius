package com.medigenius.ai;

import com.medigenius.config.MediGeniusProperties;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Replaces backend/app/tools/llm_client.py -> get_llm() singleton ChatGroq(...) client.
 *
 * OpenRouter, Groq, and OpenAI all expose an OpenAI-compatible /chat/completions API, so
 * all three are wired through LangChain4j's OpenAiChatModel by swapping the base-url +
 * api-key. Ollama uses LangChain4j's dedicated OllamaChatModel.
 *
 * Provider is selected via medigenius.llm.provider (openai | groq | ollama), matching
 * the LLM_PROVIDER env var. "openai" is the default and, per medigenius.llm.openai.base-url,
 * points at OpenRouter (https://openrouter.ai/api/v1) out of the box - Groq is only used if
 * explicitly selected.
 *
 * IMPORTANT: only builder methods that exist in LangChain4j 0.35.0's OpenAiChatModel.Builder
 * are used here (apiKey/baseUrl/modelName/temperature/maxTokens/timeout/build). Methods such
 * as defaultHeaders()/customHeaders()/extraHeaders() were removed because they do not exist
 * in this version and would fail to compile.
 */
@Configuration
@RequiredArgsConstructor
public class LlmClientConfig {

    private final MediGeniusProperties properties;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        String provider = properties.getLlm().getProvider().toLowerCase();

        return switch (provider) {
            case "ollama" -> OllamaChatModel.builder()
                    .baseUrl(properties.getLlm().getOllama().getBaseUrl())
                    .modelName(properties.getLlm().getOllama().getModelName())
                    .temperature(properties.getLlm().getTemperature())
                    .timeout(Duration.ofSeconds(120))
                    .build();

            // Explicit opt-in only - Groq's OpenAI-compatible endpoint. No longer the default.
            case "groq" -> OpenAiChatModel.builder()
                    .apiKey(properties.getLlm().getGroq().getApiKey())
                    .baseUrl(properties.getLlm().getGroq().getBaseUrl())
                    .modelName(properties.getLlm().getModelName())
                    .temperature(properties.getLlm().getTemperature())
                    .maxTokens(properties.getLlm().getMaxTokens())
                    .timeout(Duration.ofSeconds(60))
                    .build();

            // default / "openai" - OpenAI-compatible endpoint, defaults to OpenRouter via
            // medigenius.llm.openai.base-url (see application.properties).
            default -> OpenAiChatModel.builder()
                    .apiKey(properties.getLlm().getOpenai().getApiKey())
                    .baseUrl(properties.getLlm().getOpenai().getBaseUrl())
                    .modelName(properties.getLlm().getModelName())
                    .temperature(properties.getLlm().getTemperature())
                    .maxTokens(properties.getLlm().getMaxTokens())
                    .timeout(Duration.ofSeconds(60))
                    .build();
        };
    }
}
