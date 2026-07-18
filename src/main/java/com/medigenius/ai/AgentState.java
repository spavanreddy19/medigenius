package com.medigenius.ai;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Direct port of backend/app/core/state.py -> AgentState (TypedDict).
 * This is the mutable object threaded through every node of the workflow,
 * exactly like the LangGraph state dict in Python.
 */
@Data
public class AgentState {

    /** The current user question being answered. */
    private String question;

    /** Documents retrieved by the retriever/wikipedia/tavily agents. */
    private List<String> documents = new ArrayList<>();

    /** Raw generation text produced by whichever agent produced an answer. */
    private String generation;

    /** Final answer written by the executor agent. */
    private String finalAnswer;

    /** Human-readable label of which source produced the answer (shown to the frontend). */
    private String source;

    /** session id this state belongs to (used for history persistence/threading). */
    private String sessionId;

    // ── per-tool attempted/success flags, mirrors the Python dict keys exactly ──
    private boolean retrieverAttempted;
    private boolean retrieverSuccess;
    private boolean llmAttempted;
    private boolean llmSuccess;
    private boolean wikipediaAttempted;
    private boolean wikipediaSuccess;
    private boolean tavilyAttempted;
    private boolean tavilySuccess;

    /** Retry counter used to avoid infinite loops between fallback nodes. */
    private int retryCount;

    /** Name of the next node to route to, set by PlannerAgent / fallback edges (e.g. "retriever", "llm_agent"). */
    private String nextTool;

    /** Trimmed rolling conversation history: list of {role, content} maps, newest last. */
    private List<ConversationTurn> conversationHistory = new ArrayList<>();

    @Data
    public static class ConversationTurn {
        private final String role;
        private final String content;
        /** Only populated on assistant turns, mirrors Python's {"role": "assistant", ..., "source": ...}. */
        private String source;

        public ConversationTurn(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public ConversationTurn(String role, String content, String source) {
            this.role = role;
            this.content = content;
            this.source = source;
        }
    }

    /** Resets the per-turn tool flags/documents while keeping history + session id. */
    public void resetForNewQuestion(String newQuestion) {
        this.question = newQuestion;
        this.documents = new ArrayList<>();
        this.generation = null;
        this.finalAnswer = null;
        this.source = null;
        this.retrieverAttempted = false;
        this.retrieverSuccess = false;
        this.llmAttempted = false;
        this.llmSuccess = false;
        this.wikipediaAttempted = false;
        this.wikipediaSuccess = false;
        this.tavilyAttempted = false;
        this.tavilySuccess = false;
        this.retryCount = 0;
    }
}
