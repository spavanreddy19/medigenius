package com.medigenius.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * NEW ENTITY (Feature 6 - PDF Upload). Tracks every PDF a logged-in user uploads via
 * POST /api/pdf/upload, independent of the single boot-time medical_book.pdf that
 * VectorStoreService already indexes (that flow is untouched).
 */
@Entity
@Table(name = "uploaded_documents", indexes = {
        @Index(name = "idx_uploaded_documents_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadedDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    /** Path on disk under the configured uploads directory. */
    @Column(name = "stored_path", nullable = false, length = 500)
    private String storedPath;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "uploaded_at", nullable = false)
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
