package com.medigenius.ai;

/**
 * Contract implemented by every agent node (memory, planner, retriever, llm_agent,
 * wikipedia, tavily, executor, explanation). Each Python function `def node(state) -> state`
 * becomes one implementation of this interface.
 */
public interface AgentNode {

    /**
     * Mutates and returns the given state, exactly like the Python node functions
     * (`def memory_node(state: AgentState) -> AgentState`).
     */
    AgentState run(AgentState state);

    /**
     * Node name used by WorkflowEngine's routing map (mirrors the string node names
     * registered via `graph.add_node("retriever", retriever_node)` in Python).
     */
    String name();
}
