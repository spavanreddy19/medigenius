package com.medigenius.controller;

import com.medigenius.dto.ChatRequestDto;
import com.medigenius.dto.ChatResponseDto;
import com.medigenius.dto.NewChatResponseDto;
import com.medigenius.dto.SimpleMessageResponseDto;
import com.medigenius.entity.User;
import com.medigenius.security.UserPrincipal;
import com.medigenius.service.ChatService;
import com.medigenius.service.SessionIdService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Direct port of backend/app/api/v1/endpoints/chat.py.
 * Base path /api/v1 matches api_router's prefix (see api/v1/api.py) with no extra prefix
 * per-router, since the Python routers register bare paths ("/chat", "/clear", "/new-chat").
 *
 * MODIFIED (Features 4 & 5 - Chat History + Memory): this endpoint stays 100% public/anonymous
 * by default (unchanged), but if the caller also sends a valid "Authorization: Bearer <token>"
 * (see UserJwtAuthFilter, which runs on every request regardless of route auth rules), the
 * resulting chat history/title/memory is attached to that account.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SessionIdService sessionIdService;

    /** POST /api/v1/chat - equivalent of chat_endpoint(). */
    @PostMapping("/chat")
    public ChatResponseDto chat(@Valid @RequestBody ChatRequestDto request, HttpServletRequest httpRequest) {
        String sessionId = sessionIdService.resolveSessionId(httpRequest);
        return chatService.processMessage(sessionId, request.message(), currentUserOrNull());
    }

    /** POST /api/v1/clear - equivalent of clear_endpoint(). */
    @PostMapping("/clear")
    public SimpleMessageResponseDto clear(HttpServletRequest httpRequest) {
        String sessionId = sessionIdService.resolveSessionId(httpRequest);
        chatService.clearConversation(sessionId);
        return new SimpleMessageResponseDto("Conversation cleared", true);
    }

    /** POST /api/v1/new-chat - equivalent of new_chat_endpoint(). */
    @PostMapping("/new-chat")
    public NewChatResponseDto newChat(HttpServletRequest httpRequest) {
        String newSessionId = sessionIdService.createNewSessionId(httpRequest);
        return new NewChatResponseDto("New chat created", newSessionId, true);
    }

    /** NEW (Features 4/5) - resolves the logged-in User from the SecurityContext, or null. */
    private User currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUser();
        }
        return null;
    }
}
