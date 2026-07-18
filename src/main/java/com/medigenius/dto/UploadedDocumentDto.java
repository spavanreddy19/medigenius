package com.medigenius.dto;

/** NEW DTO (Feature 6/11/12 - PDF Upload / Sidebar / Profile). */
public record UploadedDocumentDto(
        Long id,
        String fileName,
        int chunkCount,
        long fileSizeBytes,
        String uploadedAt
) {
}
