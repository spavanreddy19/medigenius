package com.medigenius.controller;

import com.medigenius.dto.UploadedDocumentDto;
import com.medigenius.security.UserPrincipal;
import com.medigenius.service.PdfUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * NEW CONTROLLER (Feature 6 - PDF Upload). Protected - requires a logged-in user.
 */
@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
public class PdfController {

    private final PdfUploadService pdfUploadService;

    /** POST /api/pdf/upload (multipart/form-data, field name "pdf"). */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public UploadedDocumentDto upload(@AuthenticationPrincipal UserPrincipal principal,
                                       @RequestParam("pdf") MultipartFile pdf) {
        return pdfUploadService.upload(principal.getUser(), pdf);
    }
}
