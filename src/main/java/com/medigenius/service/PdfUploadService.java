package com.medigenius.service;

import com.medigenius.ai.VectorStoreService;
import com.medigenius.config.MediGeniusProperties;
import com.medigenius.dto.UploadedDocumentDto;
import com.medigenius.entity.UploadedDocument;
import com.medigenius.entity.User;
import com.medigenius.repository.UploadedDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * NEW SERVICE (Feature 6 - PDF Upload).
 * Handles POST /api/pdf/upload: saves the file under the configured uploads directory,
 * extracts + chunks + embeds it into the SAME in-memory vector store the existing chatbot
 * already retrieves from (via VectorStoreService.ingestDocument), and records an
 * UploadedDocument row tied to the uploading user.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfUploadService {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final MediGeniusProperties properties;
    private final VectorStoreService vectorStoreService;
    private final UploadedDocumentRepository uploadedDocumentRepository;

    @Transactional
    public UploadedDocumentDto upload(User user, MultipartFile pdf) {
        if (pdf.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        String originalName = pdf.getOriginalFilename() != null ? pdf.getOriginalFilename() : "upload.pdf";
        if (!originalName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are supported");
        }

        try {
            Path uploadDir = Path.of(properties.getAuth().getUploadDir());
            Files.createDirectories(uploadDir);

            String storedFileName = UUID.randomUUID() + "-" + sanitize(originalName);
            Path storedPath = uploadDir.resolve(storedFileName);
            pdf.transferTo(storedPath);

            int chunkCount = vectorStoreService.ingestDocument(storedPath.toFile());

            UploadedDocument document = UploadedDocument.builder()
                    .user(user)
                    .originalFileName(originalName)
                    .storedPath(storedPath.toString())
                    .chunkCount(chunkCount)
                    .fileSizeBytes(pdf.getSize())
                    .build();
            document = uploadedDocumentRepository.save(document);

            log.info("User {} uploaded '{}' ({} chunks indexed)", user.getEmail(), originalName, chunkCount);

            return new UploadedDocumentDto(
                    document.getId(),
                    document.getOriginalFileName(),
                    document.getChunkCount(),
                    document.getFileSizeBytes(),
                    document.getUploadedAt().format(ISO_FORMATTER));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save uploaded PDF: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to index uploaded PDF: " + e.getMessage(), e);
        }
    }

    private String sanitize(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
