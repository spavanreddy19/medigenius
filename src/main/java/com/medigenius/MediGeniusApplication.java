package com.medigenius;

import com.medigenius.ai.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MediGenius Backend - Spring Boot entry point.
 *
 * Equivalent of the Python project's backend/app/main.py:
 *  - FastAPI app + CORS + SessionMiddleware      -> SecurityConfig + CorsConfig (see config/security packages)
 *  - lifespan() startup hook (init DB, PDF -> vector store, compile workflow)
 *      -> this CommandLineRunner (runs once after full Spring context startup,
 *         after Hibernate has already created/validated the `messages` table
 *         via spring.jpa.hibernate.ddl-auto=update)
 *
 * Module layout (mirrors the Python package layout 1:1):
 *   controller/   - REST endpoints (api/v1/endpoints/*.py)
 *   service/      - ChatService, SessionIdService (services/*.py)
 *   repository/   - Spring Data JPA (models/message.py + services/database_service.py)
 *   entity/       - JPA entities (models/*.py)
 *   dto/          - request/response DTOs (schemas/*.py)
 *   config/       - CORS, properties (core/config.py)
 *   security/     - JWT + session filter (Starlette SessionMiddleware equivalent)
 *   exception/    - global exception handling
 *   ai/           - LangGraph workflow port (core/langgraph_workflow.py, core/state.py)
 *   ai/agents/    - the 8 agent nodes (agents/*.py)
 */
@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class MediGeniusApplication implements CommandLineRunner {

    private final VectorStoreService vectorStoreService;

    public static void main(String[] args) {
        SpringApplication.run(MediGeniusApplication.class, args);
    }

    /**
     * Startup hook - equivalent of the Python `lifespan()` context manager.
     * Loads the medical PDF (if present) into the vector store exactly once at boot.
     * Database table creation is already handled by Hibernate (ddl-auto=update).
     */
    @Override
    public void run(String... args) {
        log.info("Initializing MediGenius System...");
        //vectorStoreService.initializeFromPdfIfPresent();
        log.info("MediGenius System Ready!");
    }
}
