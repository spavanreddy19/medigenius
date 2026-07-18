package com.medigenius.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Typed binding of every medigenius.* property in application.properties.
 * Direct equivalent of backend/app/core/config.py (which loaded .env into module-level constants).
 */
@Component
@ConfigurationProperties(prefix = "medigenius")
@Getter
@Setter
public class MediGeniusProperties {

    private final Storage storage = new Storage();
    private final Llm llm = new Llm();
    private final Rag rag = new Rag();
    private final Wikipedia wikipedia = new Wikipedia();
    private final Tavily tavily = new Tavily();
    private final Chat chat = new Chat();
    private final Jwt jwt = new Jwt();
    private final Session session = new Session();
    private final Cors cors = new Cors();
    /** NEW (Features 1/3) - real user-account JWT config, distinct from the anonymous `jwt` block above. */
    private final Auth auth = new Auth();

    @Getter @Setter
    public static class Storage {
        private String logDir;
        private String vectorStoreDir;
        private String pdfPath;
    }

    @Getter @Setter
    public static class Llm {
        private String provider;
        private String modelName;
        private double temperature;
        private int maxTokens;
        private final Groq groq = new Groq();
        private final Openai openai = new Openai();
        private final Ollama ollama = new Ollama();

        @Getter @Setter
        public static class Groq {
            private String apiKey;
            private String baseUrl;
        }

        @Getter @Setter
        public static class Openai {
            private String apiKey;
            private String baseUrl;
        }

        @Getter @Setter
        public static class Ollama {
            private String baseUrl;
            private String modelName;
        }
    }

    @Getter @Setter
    public static class Rag {
        private int topK;
        private int chunkSize;
        private int chunkOverlap;
        private int minValidChars;
    }

    @Getter @Setter
    public static class Wikipedia {
        private String baseUrl;
        private int topKResults;
        private int maxDocChars;
        private int minValidChars;
    }

    @Getter @Setter
    public static class Tavily {
        private String apiKey;
        private String baseUrl;
        private int maxResults;
        private int minValidChars;
    }

    @Getter @Setter
    public static class Chat {
        private int maxHistoryTurns;
        private int historyContextWindow;
        private int executorContextWindow;
    }

    @Getter @Setter
    public static class Jwt {
        private String secret;
        private long expirationMs;
        private String headerName;
    }

    @Getter @Setter
    public static class Session {
        private String headerName;
    }

    @Getter @Setter
    public static class Cors {
        private String allowedOrigins;
    }

    /** NEW (Features 1/3) - JWT config for real, logged-in user accounts (Authorization: Bearer). */
    @Getter @Setter
    public static class Auth {
        private String jwtSecret;
        private long jwtExpirationMs;
        /** Directory PDFs uploaded via POST /api/pdf/upload are saved into. */
        private String uploadDir;
    }
}
