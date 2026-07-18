package com.medigenius.repository;

import com.medigenius.entity.UploadedDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** NEW REPOSITORY (Feature 6 - PDF Upload). */
public interface UploadedDocumentRepository extends JpaRepository<UploadedDocument, Long> {

    List<UploadedDocument> findByUserIdOrderByUploadedAtDesc(Long userId);

    long countByUserId(Long userId);
}
