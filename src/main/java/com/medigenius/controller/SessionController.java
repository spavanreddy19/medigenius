package com.medigenius.controller;

import com.medigenius.dto.HistoryResponseDto;
import com.medigenius.dto.SessionMessagesResponseDto;
import com.medigenius.dto.SessionsListResponseDto;
import com.medigenius.dto.SimpleMessageResponseDto;
import com.medigenius.service.DatabaseService;
import com.medigenius.service.SessionIdService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Direct port of backend/app/api/v1/endpoints/session.py.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SessionController {

    private static final String SESSION_ATTR = "session_id";

    private final DatabaseService databaseService;
    private final SessionIdService sessionIdService;

    /** GET /api/v1/history - equivalent of get_history_endpoint(). */
    @GetMapping("/history")
    public HistoryResponseDto getHistory(HttpServletRequest httpRequest) {
        String sessionId = sessionIdService.resolveSessionId(httpRequest);
        return new HistoryResponseDto(databaseService.getChatHistory(sessionId), true);
    }

    /** GET /api/v1/sessions - equivalent of get_sessions_endpoint(). */
    @GetMapping("/sessions")
    public SessionsListResponseDto getSessions() {
        return new SessionsListResponseDto(databaseService.getAllSessions(), true);
    }

    /** GET /api/v1/session/{sessionId} - equivalent of load_session_endpoint(). */
    @GetMapping("/session/{sessionId}")
    public SessionMessagesResponseDto loadSession(@PathVariable String sessionId, HttpServletRequest httpRequest) {
        // req.session["session_id"] = session_id -> set this as the active session
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(SESSION_ATTR, sessionId);

        return new SessionMessagesResponseDto(databaseService.getChatHistory(sessionId), sessionId, true);
    }

    /** DELETE /api/v1/session/{sessionId} - equivalent of delete_session_endpoint(). */
    @DeleteMapping("/session/{sessionId}")
    public SimpleMessageResponseDto deleteSession(@PathVariable String sessionId, HttpServletRequest httpRequest) {
        databaseService.deleteSession(sessionId);

        HttpSession session = httpRequest.getSession(true);
        Object activeSessionId = session.getAttribute(SESSION_ATTR);
        if (sessionId.equals(activeSessionId)) {
            session.setAttribute(SESSION_ATTR, UUID.randomUUID().toString());
        }

        return new SimpleMessageResponseDto("Session deleted", true);
    }
}
