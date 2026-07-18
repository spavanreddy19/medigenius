package com.medigenius.service;

import com.medigenius.dto.UploadedDocumentDto;
import com.medigenius.dto.UserProfileDto;
import com.medigenius.dto.UserResponseDto;
import com.medigenius.entity.User;
import com.medigenius.repository.ConversationRepository;
import com.medigenius.repository.UploadedDocumentRepository;
import com.medigenius.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * NEW SERVICE (Feature 2 - User Entity).
 * CRUD + read-model helpers for User accounts. Registration/login business logic itself
 * lives in {@link AuthenticationService}; this class is the plain data-access layer.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final UploadedDocumentRepository uploadedDocumentRepository;

    @Transactional(readOnly = true)
    public User getByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }

    public UserResponseDto toResponseDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt().format(ISO_FORMATTER));
    }

    /** Equivalent of GET /api/users/me - Feature 12 (Profile). */
    @Transactional(readOnly = true)
    public UserProfileDto getProfile(Long userId) {
        User user = getByIdOrThrow(userId);

        long totalConversations = conversationRepository.countByUserId(userId);

        List<UploadedDocumentDto> documents = uploadedDocumentRepository.findByUserIdOrderByUploadedAtDesc(userId)
                .stream()
                .map(doc -> new UploadedDocumentDto(
                        doc.getId(),
                        doc.getOriginalFileName(),
                        doc.getChunkCount(),
                        doc.getFileSizeBytes(),
                        doc.getUploadedAt().format(ISO_FORMATTER)))
                .toList();

        return new UserProfileDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt().format(ISO_FORMATTER),
                totalConversations,
                documents);
    }
}
