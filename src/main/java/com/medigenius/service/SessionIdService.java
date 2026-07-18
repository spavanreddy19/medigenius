package com.medigenius.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.medigenius.config.MediGeniusProperties;

import java.util.UUID;

/**
 * Replaces the `_get_session_id(request)` helper duplicated across
 * backend/app/api/v1/endpoints/chat.py and session.py, plus Starlette's
 * SessionMiddleware (signed cookie session) used in main.py.
 *
 * Behavior preserved exactly:
 *   1. If the client sends an `X-Session-ID` header, use it verbatim.
 *   2. Otherwise, read/create a "session_id" attribute on the server-side session
 *      (Spring's HttpSession, backed by the standard JSESSIONID cookie - functionally
 *      equivalent to Starlette's cookie-based session store).
 */
@Service
@RequiredArgsConstructor
public class SessionIdService {

    private final MediGeniusProperties properties;
    private static final String SESSION_ATTR = "session_id";

    public String resolveSessionId(HttpServletRequest request) {
        String headerSessionId = request.getHeader(properties.getSession().getHeaderName());
        if (headerSessionId != null && !headerSessionId.isBlank()) {
            return headerSessionId;
        }

        HttpSession session = request.getSession(true);
        Object existing = session.getAttribute(SESSION_ATTR);
        if (existing != null) {
            return existing.toString();
        }

        String newId = UUID.randomUUID().toString();
        session.setAttribute(SESSION_ATTR, newId);
        return newId;
    }

    /**
     * Equivalent of the new-chat endpoint's `req.session["session_id"] = new_id`.
     * Overwrites the server-side session with a freshly generated id and returns it.
     */
    public String createNewSessionId(HttpServletRequest request) {
        String newId = UUID.randomUUID().toString();
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_ATTR, newId);
        return newId;
    }
}
