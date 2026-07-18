package com.medigenius.ai;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.medigenius.config.MediGeniusProperties;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Replaces:
 *   - backend/app/tools/pdf_loader.py  (PyPDFLoader + 512/128-token RecursiveCharacterTextSplitter)
 *   - backend/app/tools/vector_store.py (create/load vector store, get_retriever(k=3))
 *
 * Backed by an in-memory {@link EmbeddingStore} (see VectorStoreConfig) - no external
 * vector database or Docker container required. Runs once at boot (see
 * MediGeniusApplication.run()) to keep the vector index in sync with the Python behavior
 * of building the store the first time medical_book.pdf is seen. Because the store is
 * in-memory, this rebuild happens on every restart (the "already populated" check below
 * only guards against double-indexing within the same running process, e.g. if invoked
 * twice).
 */
@Slf4j
@Service
@ConditionalOnProperty(
        prefix = "medigenius.rag",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@RequiredArgsConstructor
public class VectorStoreService {

    private final MediGeniusProperties properties;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    /**
     * Equivalent of the Python startup step that loads medical_book.pdf, splits it into
     * 512-token/128-overlap chunks, embeds them, and upserts into Chroma - but only if
     * the collection doesn't already contain data (avoids re-indexing on every restart).
     */
    public void initializeFromPdfIfPresent() {
        File pdfFile = new File(properties.getStorage().getPdfPath());
        if (!pdfFile.exists()) {
            log.warn("PDF not found at {} - skipping RAG index build (retriever agent will simply fail over).", pdfFile.getAbsolutePath());
            return;
        }

        if (collectionAlreadyPopulated()) {
            log.info("Vector store already populated - skipping re-index.");
            return;
        }

        try {
            String rawText = extractText(pdfFile);
            Document document = Document.from(rawText);

            DocumentSplitter splitter = DocumentSplitters.recursive(
                    properties.getRag().getChunkSize(),
                    properties.getRag().getChunkOverlap()
            );
            List<TextSegment> segments = splitter.split(document);

            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            embeddingStore.addAll(embeddings, segments);

            log.info("Indexed {} chunks from {} into the vector store.", segments.size(), pdfFile.getName());
        } catch (Exception e) {
            log.error("Failed to build vector store from PDF: {}", e.getMessage(), e);
        }
    }

    /**
     * Equivalent of retriever.invoke(question) with k=3 in agents/retriever.py,
     * filtered down to documents with more than `medigenius.rag.min-valid-chars` characters,
     * exactly mirroring the Python retriever agent's validity check.
     */
    public List<String> retrieveRelevantDocuments(String question) {
        Embedding queryEmbedding = embeddingModel.embed(question).content();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(properties.getRag().getTopK())
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

        return result.matches().stream()
                .map(EmbeddingMatch::embedded)
                .map(TextSegment::text)
                .filter(text -> text != null && text.length() > properties.getRag().getMinValidChars())
                .collect(Collectors.toList());
    }

    /**
     * NEW (Feature 6 - PDF Upload). Reuses the exact same extraction/chunking/embedding
     * pipeline as {@link #initializeFromPdfIfPresent()} above, but callable on demand for a
     * single user-uploaded file, and returns the chunk count instead of just logging it.
     * Chunks are upserted into the SAME in-memory EmbeddingStore used for the boot-time
     * medical_book.pdf, so uploaded PDFs become searchable by the existing retriever agent too
     * (for the lifetime of the running process).
     */
    public int ingestDocument(File pdfFile) throws Exception {
        String rawText = extractText(pdfFile);
        Document document = Document.from(rawText);

        DocumentSplitter splitter = DocumentSplitters.recursive(
                properties.getRag().getChunkSize(),
                properties.getRag().getChunkOverlap()
        );
        List<TextSegment> segments = splitter.split(document);

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingStore.addAll(embeddings, segments);

        log.info("Indexed {} chunks from uploaded file {} into the vector store.", segments.size(), pdfFile.getName());
        return segments.size();
    }

    private boolean collectionAlreadyPopulated() {
        try {
            // Cheap probe: search for an arbitrary embedding and see if anything comes back.
            Embedding probe = embeddingModel.embed("medigenius-init-probe").content();
            EmbeddingSearchRequest probeRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(probe)
                    .maxResults(1)
                    .build();
            return !embeddingStore.search(probeRequest).matches().isEmpty();
        } catch (Exception e) {
            log.debug("Vector store probe failed (likely empty/unreachable collection): {}", e.getMessage());
            return false;
        }
    }

    private String extractText(File pdfFile) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
