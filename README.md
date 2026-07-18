# MediGenius — Java/Spring Boot Migration

Production-ready Java 21 / Spring Boot 3 port of the original Python/FastAPI **MediGenius**
medical consultation chatbot, preserving all business logic, API behavior, and the
existing React frontend.

## Quick start (IntelliJ IDEA)

1. **File → Open** this folder (`medigenius-java`) - IntelliJ will detect the Maven project automatically.
2. Set environment variables (`GROQ_API_KEY` at minimum) either in your run configuration
   or via a local `application-local.properties` / OS env vars.
3. Start MySQL and ChromaDB (`docker compose up mysql chroma -d`), or point
   `DB_HOST`/`CHROMA_BASE_URL` at your own instances.
4. Run:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
5. Backend is live at `http://localhost:8000/api/v1`. See `docs/API_DOCUMENTATION.md`.

## Full stack via Docker

```bash
cp .env.example .env   # fill in GROQ_API_KEY, JWT_SECRET, etc.
docker compose up --build
```

Brings up: MySQL, ChromaDB, the Spring Boot backend, and the original React frontend
(nginx-served build) - see `docs/FRONTEND_INTEGRATION_GUIDE.md`.

## Project layout

```
src/main/java/com/medigenius/
├── controller/    ChatController, SessionController, HealthController
├── service/       ChatService, DatabaseService, SessionIdService
├── repository/    MessageRepository (Spring Data JPA)
├── entity/        Message
├── dto/           8 request/response records mirroring the Pydantic schemas
├── config/        MediGeniusProperties, WebConfig (CORS)
├── security/       JwtUtil, JwtAuthFilter, SecurityConfig (additive, non-breaking)
├── exception/     GlobalExceptionHandler + custom exceptions
├── ai/            AgentState, AgentNode, WorkflowEngine (hand-rolled LangGraph port),
│                  LlmClientConfig, VectorStoreConfig/Service, WikipediaClient, TavilyClient
├── ai/agents/     8 node classes: memory, planner, llm_agent, retriever, wikipedia,
│                  tavily, executor, explanation
└── MediGeniusApplication.java
```

## Key migration decisions (see full analysis in the chat conversation)

- **LangGraph → hand-rolled `WorkflowEngine`**: LangChain4j has no LangGraph equivalent, so
  the exact node/edge topology of `core/langgraph_workflow.py` was reproduced as a plain
  Java state machine - including a source-verified quirk where the original graph's
  `wikipedia`/`tavily` nodes are registered but never reachable by any wired edge. This was
  preserved as-is rather than silently fixed, per the "keep business logic exactly the
  same" requirement. See `WorkflowEngine.java`'s javadoc for how to intentionally enable
  that fallback chain if desired.
- **Groq/OpenAI/Ollama**: unified behind LangChain4j's `OpenAiChatModel` (Groq and OpenAI are
  both OpenAI-API-compatible) and `OllamaChatModel`, selected via `medigenius.llm.provider`.
- **Embeddings**: LangChain4j's `AllMiniLmL6V2EmbeddingModel` is the same model
  (`all-MiniLM-L6-v2`, 384-dim, local/offline) used by the Python HuggingFace embeddings.
- **Vector store**: dockerized ChromaDB via `langchain4j-chroma`, same persistence model.
- **Auth**: the Python app has no authentication at all. Spring Security + JWT were added
  as an **opt-in, non-breaking layer** (`SecurityConfig` keeps every route `permitAll()`)
  to satisfy the target stack without changing observable API behavior.
- **Session identity**: `X-Session-ID` header takes priority (unchanged), otherwise falls
  back to a Spring `HttpSession` (functional equivalent of Starlette's signed cookie session).

## Docs

- `docs/API_DOCUMENTATION.md` — full endpoint reference with request/response/error JSON
- `docs/FRONTEND_INTEGRATION_GUIDE.md` — how the existing React app connects, unmodified
- `src/main/resources/db/schema.sql` — MySQL DDL + sample data
