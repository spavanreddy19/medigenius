package com.medigenius.ai;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Replaces backend/app/tools/vector_store.py:
 *   - get_embeddings() -> singleton HuggingFaceEmbeddings("all-MiniLM-L6-v2")
 *   - get_vector_store()/get_retriever() -> singleton Chroma persistent client
 *
 * AllMiniLmL6V2EmbeddingModel bundles the exact same all-MiniLM-L6-v2 weights (ONNX
 * runtime, runs fully local/offline, no API key) - a true 1:1 replacement, not an
 * approximation, so embedding vectors are compatible in dimensionality (384) with
 * whatever was previously indexed.
 *
 * Chroma has been completely removed (the latest Chroma Docker image is incompatible
 * with LangChain4j 0.35.0's ChromaEmbeddingStore, which was returning HTTP 405 on
 * every call). It is replaced with LangChain4j's InMemoryEmbeddingStore, which requires
 * no external service/Docker container at all - the app now runs standalone with
 * `mvn spring-boot:run`.
 *
 * TRADE-OFF: InMemoryEmbeddingStore does not persist across restarts. The index is
 * rebuilt automatically at boot from the configured PDF (see VectorStoreService)
 * and grows as users upload new PDFs during the process's lifetime, but is lost
 * when the app restarts. If persistence across restarts becomes a requirement,
 * InMemoryEmbeddingStore also exposes serializeToJson()/writeToFile() /
 * fromFile() helpers that could be wired into a scheduled snapshot or a
 * shutdown/startup hook without switching stores again.
 */
@Configuration
public class VectorStoreConfig {

    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }
}
