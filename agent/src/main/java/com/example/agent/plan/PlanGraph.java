package com.example.agent.plan;

import java.util.List;

/**
 * Phase 6 — a plan as a directed acyclic graph the orchestrator produces <em>before</em> any
 * execution. This is the one genuinely-new concept of the phase: decomposition (plan-then-execute)
 * made into a visible artifact. We never run it — we draw it.
 *
 * <p>Nodes are sub-tasks; edges are data dependencies ({@link PlanNode#inputs()}). Independent
 * nodes (no shared inputs) could run in parallel; a node lists the ids it needs in {@code inputs}.
 *
 * @param goal  the goal, as restated by the planner
 * @param nodes the steps of the plan
 */
public record PlanGraph(String goal, List<PlanNode> nodes) {

    /**
     * One step in the plan.
     *
     * @param id         unique short identifier (referenced by other nodes' {@code inputs})
     * @param specialist which specialist would run it — an MCP server name, or "orchestrator" for
     *                   pure reasoning/aggregation steps that use no tool
     * @param op         the tool name to call, or a short verb (e.g. "compare", "select") for
     *                   orchestrator steps
     * @param summary    short human-readable description
     * @param inputs     ids of the nodes this step depends on (empty if none)
     */
    public record PlanNode(String id, String specialist, String op, String summary, List<String> inputs) {
        public List<String> inputs() {
            return inputs == null ? List.of() : inputs;
        }
    }
}
