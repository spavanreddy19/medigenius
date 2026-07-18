# MediGenius API Documentation (Java/Spring Boot)

Base URL: `http://localhost:8000/api/v1`

All endpoints preserve the exact request/response shapes of the original FastAPI service.
Session identity is resolved via the `X-Session-ID` header (preferred) or a server-side
session cookie (`JSESSIONID`), exactly like the Python app's `X-Session-ID`/cookie logic.

---

## GET /health

Health check.

**Response 200**
```json
{ "status": "healthy", "service": "MediGenius Backend v2" }
```

---

## POST /chat

Send a user message through the agentic pipeline.

**Headers:** `X-Session-ID: <optional>`

**Request**
```json
{ "message": "I have had a headache and mild fever, what should I do?" }
```

**Response 200**
```json
{
  "response": "I understand that's uncomfortable. Rest, hydration, and...",
  "source": "Medical Knowledge Base",
  "timestamp": "02:35 PM",
  "success": true
}
```

**Error 422 (blank message)**
```json
{ "success": false, "error": "message: message must not be blank", "timestamp": "...", "status": 422 }
```

---

## POST /clear

Clear the in-memory conversation state (does NOT delete DB history) for the current session.

**Response 200**
```json
{ "message": "Conversation cleared", "success": true }
```

---

## POST /new-chat

Start a fresh session (generates a new session id).

**Response 200**
```json
{ "message": "New chat created", "session_id": "a1b2c3d4-...", "success": true }
```

---

## GET /history

Get persisted chat history for the current session.

**Response 200**
```json
{
  "messages": [
    { "role": "user", "content": "...", "source": null, "timestamp": "2026-07-16T09:00:00" },
    { "role": "assistant", "content": "...", "source": "AI Language Model", "timestamp": "2026-07-16T09:00:04" }
  ],
  "success": true
}
```

---

## GET /sessions

List all sessions with a preview of the last user message.

**Response 200**
```json
{
  "sessions": [
    { "sessionId": "a1b2...", "preview": "I have had a headache and mild feve...", "lastActive": "2026-07-16T09:00:00" }
  ],
  "success": true
}
```

---

## GET /session/{sessionId}

Load a specific session's history and mark it as active.

**Response 200**
```json
{
  "messages": [ { "role": "user", "content": "...", "source": null, "timestamp": "..." } ],
  "sessionId": "a1b2c3d4-...",
  "success": true
}
```

---

## DELETE /session/{sessionId}

Delete a session's history. If it was the active session, a new one is generated.

**Response 200**
```json
{ "message": "Session deleted", "success": true }
```

---

## Error Response Envelope (all endpoints)

```json
{
  "success": false,
  "error": "human-readable message",
  "timestamp": "2026-07-16T14:35:00.123",
  "status": 500
}
```

| Status | Meaning |
|---|---|
| 422 | Validation failure (e.g. blank `message`) |
| 404 | Resource not found (reserved for future use) |
| 503 | AI workflow temporarily unavailable |
| 500 | Unhandled server error |
| 401 | Missing/invalid/expired JWT, or wrong login credentials |
| 403 | Valid JWT, but insufficient permission |
| 409 | Email already registered |
| 413 | Uploaded PDF exceeds the 25MB limit |

---
---

# NEW - Authentication & Account Endpoints (Features 1-6, 11, 12)

Everything below is **additive**. All endpoints documented above keep working completely
unauthenticated, exactly as before. These new endpoints layer real user accounts on top.

Authenticate by sending the token returned from register/login as a header on every
subsequent request:

```
Authorization: Bearer <token>
```

## POST /api/auth/register

Create a new account.

**Request**
```json
{ "name": "Ada Lovelace", "email": "ada@example.com", "password": "correct-horse-battery" }
```

**Response 201**
```json
{
  "token": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "user": { "id": 1, "name": "Ada Lovelace", "email": "ada@example.com", "role": "USER", "createdAt": "2026-07-17T10:00:00" }
}
```
Errors: `409` if the email is already registered, `422` on validation failure (password < 6 chars, invalid email, etc).

## POST /api/auth/login

**Request**
```json
{ "email": "ada@example.com", "password": "correct-horse-battery" }
```

**Response 200**: same shape as register. `401` on wrong email/password.

## GET /api/users/me  *(requires Authorization header)*

Profile data for Feature 12 (sidebar user profile page).

**Response 200**
```json
{
  "id": 1,
  "name": "Ada Lovelace",
  "email": "ada@example.com",
  "role": "USER",
  "createdAt": "2026-07-17T10:00:00",
  "totalConversations": 4,
  "uploadedDocuments": [
    { "id": 3, "fileName": "labs.pdf", "chunkCount": 42, "fileSizeBytes": 183921, "uploadedAt": "2026-07-17T10:05:00" }
  ]
}
```

## GET /api/conversations  *(requires Authorization header)*

This account's titled conversation history (Feature 4/11 sidebar), newest first.

**Response 200**
```json
[
  { "id": 4, "sessionId": "11111111-...", "title": "What are the symptoms of fever?", "createdAt": "2026-07-17T10:10:00" }
]
```

## POST /api/pdf/upload  *(requires Authorization header, multipart/form-data)*

Upload a PDF; it's saved to disk, chunked, embedded, and added to the SAME vector store
the chatbot already retrieves from - so it becomes searchable in chat immediately.

**Form field**: `pdf` (file, `.pdf` only, max 25MB)

**Response 200**
```json
{ "id": 3, "fileName": "labs.pdf", "chunkCount": 42, "fileSizeBytes": 183921, "uploadedAt": "2026-07-17T10:05:00" }
```

## POST /api/v1/chat  *(Authorization header now optional)*

Unchanged request/response shape. If a valid `Authorization: Bearer <token>` is also sent,
the resulting messages are additionally tied to that account (a `Conversation` row is
created/reused, titled from the first message) instead of being purely anonymous.

