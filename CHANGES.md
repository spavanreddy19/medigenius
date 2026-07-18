# MediGenius — Change Log

Everything below is **additive** to your existing project. No existing AI feature (PDF
indexing, ChromaDB, LangChain4j workflow, chat APIs, React UI) was removed or rewritten —
only extended. Anonymous/guest chat still works exactly as before with zero login required.

## ⚠️ Action required from you
1. **Rotate your Groq API key.** It was committed in plaintext in `application.properties`
   and has been moved to the `GROQ_API_KEY` env var — but the *old* key is still valid until
   you revoke it on Groq's dashboard.
2. Set `AUTH_JWT_SECRET` (32+ random bytes) in your `.env` before running — see `.env.example`.
3. Run `mvn clean install` yourself to compile — this sandbox has no Maven Central access, so
   the Java side could not be compiler-verified here (the frontend *was* built/tested here —
   see below). Java was reviewed carefully by hand and reuses your existing, already-working
   patterns throughout, but please build before deploying.

---

## Feature 1 & 3 — JWT Authentication
| File | Status |
|---|---|
| `security/UserJwtService.java` | NEW — issues/validates `Authorization: Bearer` JWTs |
| `security/UserJwtAuthFilter.java` | NEW — authenticates requests carrying that header |
| `security/UserPrincipal.java`, `CustomUserDetailsService.java` | NEW — Spring Security glue |
| `security/SecurityConfig.java` | MODIFIED — adds the new filter, `PasswordEncoder`/`AuthenticationManager` beans, and route rules (new endpoints protected, all pre-existing `/api/v1/**` untouched/public) |
| `service/AuthenticationService.java` | NEW — register/login logic |
| `controller/AuthController.java` | NEW — `POST /api/auth/register`, `POST /api/auth/login` |
| `exception/EmailAlreadyExistsException.java`, `InvalidCredentialsException.java` | NEW |
| `exception/GlobalExceptionHandler.java` | MODIFIED — handles the above + Spring Security auth errors |

Kept your existing anonymous `JwtUtil`/`JwtAuthFilter` (X-Session-Token) completely untouched.

## Feature 2 — User Entity
`entity/User.java`, `repository/UserRepository.java`, `service/UserService.java` — all NEW.

## Feature 4 — Chat History
`entity/Conversation.java` (NEW), `repository/ConversationRepository.java` (NEW),
`service/ConversationService.java` (NEW), `controller/ConversationController.java` (NEW —
`GET /api/conversations`). `entity/Message.java` MODIFIED to add nullable `userId`/
`conversationId` columns. `service/ChatService.java` MODIFIED with an overloaded
`processMessage(sessionId, message, user)` — original 2-arg signature preserved.
`controller/ChatController.java` MODIFIED to resolve the optional logged-in user.

## Feature 5 — Conversation Memory
`service/ChatService.java` — new sessions now hydrate `AgentState.conversationHistory` from
MySQL (`DatabaseService.getChatHistory`) the first time a session is touched, so memory
survives app restarts instead of only living in the process-local map.

## Feature 6 — PDF Upload
`ai/VectorStoreService.java` MODIFIED (new public `ingestDocument()` method, reuses your
existing chunk/embed pipeline). `entity/UploadedDocument.java`,
`repository/UploadedDocumentRepository.java`, `service/PdfUploadService.java`,
`controller/PdfController.java` — all NEW (`POST /api/pdf/upload`).

## Feature 7 & 8 — Voice Input / Text-to-Speech
`frontend/src/pages/ChatPage.jsx` — mic button wired to the Web Speech API
(`SpeechRecognition`), 🔊 Speak button on every AI message wired to `speechSynthesis`.
Both feature-detected, so unsupported browsers just hide the buttons gracefully.

## Feature 9 — Responsive UI
Your existing responsive/dark-mode/glass design was already solid — kept as-is. Added a
mobile backdrop-close behavior and new pages that match the same design tokens.

## Feature 10 — Authentication Pages
`frontend/src/pages/LoginPage.jsx`, `RegisterPage.jsx`, `ForgotPasswordPage.jsx` (UI-only —
no reset-email endpoint was requested), `components/ProtectedRoute.jsx`,
`context/AuthContext.jsx` — all NEW. `App.jsx` MODIFIED into a thin router shell.

## Feature 11 — Sidebar
`frontend/src/pages/ChatPage.jsx` — sidebar now also shows **Uploaded PDFs** and an
auth-aware footer (Profile + Logout when logged in, Login/Signup when not).

## Feature 12 — Profile
`controller/UserController.java` (NEW — `GET /api/users/me`), `dto/UserProfileDto.java`,
`frontend/src/pages/ProfilePage.jsx` + `ProfilePage.css` — all NEW.

## Feature 13 — Database
`src/main/resources/db/schema.sql` MODIFIED — added `users`, `conversations`,
`uploaded_documents` tables and the two new nullable `messages` columns.

## Feature 14 — API Documentation
`docs/API_DOCUMENTATION.md` MODIFIED — new section documenting every endpoint above.

## Feature 15 — Frontend API Layer
`frontend/src/api/client.js` (NEW — axios instance, JWT request interceptor, 401 →
clear-token-and-redirect-to-login interceptor), `frontend/src/api/endpoints.js` (NEW — named
wrappers for every call). `ChatPage.jsx` now calls these instead of bare `fetch()`.

## Feature 16 — Code Quality
DTOs as records with `jakarta.validation` annotations, service/repository/controller
separation kept consistent with your existing style, `GlobalExceptionHandler` extended
rather than duplicated, `.gitignore` added (protects `.env`, `uploads/`, `target/`).

---

## Verified in this sandbox
- ✅ `frontend`: `npm install`, `vitest run` (4/4 tests pass, rewritten to mock the new
  axios API layer instead of `fetch`), `vite build` (production build succeeds, 265 modules).
- ⚠️ Backend: could not run `mvn compile` here (no Maven Central network access in this
  sandbox) — please build locally before deploying.
