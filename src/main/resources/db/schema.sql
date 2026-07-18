-- ==============================================================================
-- MediGenius - MySQL Schema
-- Equivalent of backend/app/models/message.py (SQLAlchemy Base.metadata.create_all())
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS medigenius
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE medigenius;

-- ------------------------------------------------------------------------------
-- Table: messages
-- Direct 1:1 port of the Python Message ORM model.
-- MODIFIED (Feature 4/5): added nullable user_id / conversation_id (see below).
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS messages (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id      VARCHAR(255) NOT NULL,
    role            VARCHAR(50)  NOT NULL,
    content         TEXT         NOT NULL,
    source          VARCHAR(255) NULL,
    timestamp       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    user_id         BIGINT       NULL,
    conversation_id BIGINT       NULL,

    CONSTRAINT chk_messages_role CHECK (role IN ('user', 'assistant'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Index used by every session-scoped query (history, delete, workflow lookups)
CREATE INDEX idx_messages_session_id ON messages (session_id);
CREATE INDEX idx_messages_user_id ON messages (user_id);
CREATE INDEX idx_messages_conversation_id ON messages (conversation_id);

-- Composite index accelerating the "latest message per session" aggregate query
-- used by GET /api/v1/sessions (DatabaseService.getAllSessions()).
CREATE INDEX idx_messages_session_role_timestamp ON messages (session_id, role, timestamp);

-- ------------------------------------------------------------------------------
-- Table: users (NEW - Feature 2 / Feature 13)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,   -- BCrypt hash, never plaintext
    role        VARCHAR(30)  NOT NULL DEFAULT 'USER',
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- Table: conversations (NEW - Feature 4 / Feature 13)
-- Titled, user-owned wrapper around an existing chat session_id.
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS conversations (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    session_id  VARCHAR(255) NOT NULL UNIQUE,
    title       VARCHAR(255) NOT NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_conversations_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_conversations_user_id ON conversations (user_id);

-- ------------------------------------------------------------------------------
-- Table: uploaded_documents (NEW - Feature 6 / Feature 13)
-- Per-user PDFs uploaded via POST /api/pdf/upload.
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS uploaded_documents (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    original_file_name  VARCHAR(255) NOT NULL,
    stored_path         VARCHAR(500) NOT NULL,
    chunk_count         INT          NOT NULL,
    file_size_bytes     BIGINT       NOT NULL,
    uploaded_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_uploaded_documents_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_uploaded_documents_user_id ON uploaded_documents (user_id);

-- ------------------------------------------------------------------------------
-- Sample data (optional - useful for local development / demoing the sidebar)
-- ------------------------------------------------------------------------------
INSERT INTO messages (session_id, role, content, source, timestamp) VALUES
('11111111-1111-1111-1111-111111111111', 'user',      'I have had a headache and mild fever since yesterday, what should I do?', NULL, '2026-07-10 09:00:00'),
('11111111-1111-1111-1111-111111111111', 'assistant', 'I understand that''s uncomfortable. A headache with mild fever can often be managed with rest, hydration, and over-the-counter pain relief, but please monitor your temperature and see a doctor if it persists beyond 2-3 days or worsens.', 'Medical Knowledge Base', '2026-07-10 09:00:04'),
('22222222-2222-2222-2222-222222222222', 'user',      'What are common side effects of ibuprofen?', NULL, '2026-07-11 14:22:00'),
('22222222-2222-2222-2222-222222222222', 'assistant', 'Common side effects of ibuprofen include stomach upset, heartburn, and mild dizziness. Taking it with food can help reduce stomach irritation. Let me know if you''d like more detail.', 'AI Language Model', '2026-07-11 14:22:05'),
('33333333-3333-3333-3333-333333333333', 'user',      'Tell me about the latest COVID variant news', NULL, '2026-07-12 18:05:00'),
('33333333-3333-3333-3333-333333333333', 'assistant', 'Here is a summary based on recent public health reporting on circulating variants and general precautions to consider.', 'Web Search Results', '2026-07-12 18:05:09');
