package com.medigenius.ai;

import com.medigenius.ai.agents.ExecutorAgent;
import com.medigenius.ai.agents.LlmAgent;
import com.medigenius.ai.agents.MemoryAgent;
import com.medigenius.ai.agents.PlannerAgent;
import com.medigenius.ai.agents.RetrieverAgent;
import com.medigenius.ai.agents.TavilyAgent;
import com.medigenius.ai.agents.WikipediaAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Hand-rolled replacement for LangGraph's compiled StateGraph
 * (backend/app/core/langgraph_workflow.py -> create_workflow()).
 *
 * LangChain4j has no LangGraph equivalent, so this class reproduces the exact node/edge
 * topology of the Python graph as a plain Java state machine:
 *
 *   entry point: memory
 *   memory -> planner                                    (unconditional)
 *   planner -> retriever | llm_agent                      (_route_after_planner)
 *   llm_agent -> executor | retriever                     (_route_after_llm)
 *   retriever -> executor | llm_agent                      (_route_after_rag)
 *   wikipedia -> executor | tavily                         (_route_after_wiki)
 *   tavily -> executor                                     (_route_after_tavily, always)
 *   executor -> END
 *
 * IMPORTANT - PRESERVED-AS-IS BEHAVIORAL NOTE:
 * In the original Python graph, the "wikipedia" and "tavily" nodes are registered
 * (workflow.add_node(...)) but NO edge in the compiled graph ever transitions into them -
 * only planner/llm_agent/retriever/executor are actually reachable. A `_route_after_llm_fallback`
 * routing function exists in the source that *would* route llm_agent's failure to "wikipedia",
 * but it is never wired into add_conditional_edges(), so it's dead code in the original.
 * Per the instruction to keep business logic EXACTLY the same, this engine reproduces that
 * as-built topology (wikipedia/tavily nodes are present but unreachable) rather than
 * silently "fixing" what looks like an unfinished feature. If you want the wikipedia/tavily
 * fallback chain to actually activate, tell me and I will wire llm_agent's failure branch to
 * "wikipedia" (a one-line change in routeAfterLlm below) as an intentional deviation.
 *
 * A max-iteration guard is added since llm_agent <-> retriever can theoretically ping-pong
 * forever if both keep failing - LangGraph itself would raise GraphRecursionError in that
 * case; here we cap and fail safely into the executor instead.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEngine {

    private static final String END = "END";
    private static final int MAX_ITERATIONS = 25;

    private final MemoryAgent memoryAgent;
    private final PlannerAgent plannerAgent;
    private final LlmAgent llmAgent;
    private final RetrieverAgent retrieverAgent;
    private final WikipediaAgent wikipediaAgent;
    private final TavilyAgent tavilyAgent;
    private final ExecutorAgent executorAgent;

    /**
     * Runs the full graph from the "memory" entry point to "executor" -> END,
     * exactly mirroring `create_workflow().invoke(state)` in Python's chat_service.py.
     */
    public AgentState invoke(AgentState state) {
        String current = "memory";
        int iterations = 0;

        while (!END.equals(current)) {
            if (++iterations > MAX_ITERATIONS) {
                log.error("WorkflowEngine exceeded max iterations ({}) - forcing executor to avoid infinite loop.", MAX_ITERATIONS);
                current = "executor";
            }

            switch (current) {
                case "memory" -> {
                    memoryAgent.run(state);
                    current = "planner";
                }
                case "planner" -> {
                    plannerAgent.run(state);
                    current = routeAfterPlanner(state);
                }
                case "llm_agent" -> {
                    llmAgent.run(state);
                    current = routeAfterLlm(state);
                }
                case "retriever" -> {
                    retrieverAgent.run(state);
                    current = routeAfterRag(state);
                }
                case "wikipedia" -> {
                    wikipediaAgent.run(state);
                    current = routeAfterWiki(state);
                }
                case "tavily" -> {
                    tavilyAgent.run(state);
                    current = routeAfterTavily();
                }
                case "executor" -> {
                    executorAgent.run(state);
                    current = END;
                }
                default -> throw new IllegalStateException("Unknown workflow node: " + current);
            }
        }

        return state;
    }

    // ── Routing functions - direct ports of the Python `_route_after_*` functions ──

    private String routeAfterPlanner(AgentState state) {
        return "retriever".equals(state.getNextTool()) ? "retriever" : "llm_agent";
    }

    private String routeAfterLlm(AgentState state) {
        return state.isLlmSuccess() ? "executor" : "retriever";
    }

    private String routeAfterRag(AgentState state) {
        return state.isRetrieverSuccess() ? "executor" : "llm_agent";
    }

    private String routeAfterWiki(AgentState state) {
        return state.isWikipediaSuccess() ? "executor" : "tavily";
    }

    private String routeAfterTavily() {
        return "executor";
    }
}
