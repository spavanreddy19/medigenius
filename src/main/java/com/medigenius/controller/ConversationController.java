package com.medigenius.controller;

import com.medigenius.dto.ConversationDto;
import com.medigenius.security.UserPrincipal;
import com.medigenius.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * NEW CONTROLLER (Feature 4 - Chat History / Feature 11 - Sidebar).
 * Protected. Note: the pre-existing GET /api/v1/sessions endpoint (SessionController)
 * still lists ALL sessions across everyone, unauthenticated, exactly as before - this new
 * endpoint is the account-scoped equivalent used by the new sidebar "Conversation History"
 * section once a user is logged in.
 */
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /** GET /api/conversations - this user's titled conversation list, newest first. */
    @GetMapping
    public List<ConversationDto> myConversations(@AuthenticationPrincipal UserPrincipal principal) {
        return conversationService.listForUser(principal.getId());
    }
}
