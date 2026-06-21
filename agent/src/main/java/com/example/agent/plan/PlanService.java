package com.example.agent.plan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.agent.mcp.ToolCatalog;
import com.example.agent.usage.Pricing;
import com.example.agent.usage.RunUsage;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

/**
 * Phase 6 — "Watch it Plan". Turns a goal into a {@link PlanGraph} with ONE tool-less model call:
 * the LLM's whole contribution is decomposition. We deliberately do not execute the plan — the
 * graph itself is the artifact (rendered in the browser).
 *
 * <p>Why no execution and no sub-agent LLMs: in earlier drafts each sub-task spawned its own LLM
 * that just called one tool — pure overhead. Here the honest lesson stands alone: a model is good
 * at turning a fuzzy goal into a structured, dependency-aware plan over the available tools. The
 * tools are discovered over MCP ({@link ToolCatalog}); the planner composes them but never calls them.
 */
@Service
public class PlanService {

    private static final Logger log = LoggerFactory.getLogger(PlanService.class);

    private static final String PLANNER_PROMPT = """
            You are a planning agent. Given a user's goal, produce a PLAN as a directed acyclic
            graph of steps. You do NOT execute anything — you only design the plan.

            Each step is a node with:
              - "id": a unique short snake_case identifier
              - "specialist": who would run it — one of the MCP server names listed below, or
                "orchestrator" for pure reasoning / aggregation / decision steps that use no tool
              - "op": the tool name to call (for a specialist), or a short verb like "compare",
                "select" or "report" (for orchestrator steps)
              - "summary": a short human-readable description of the step
              - "inputs": the ids of the steps whose results this step depends on (empty if none)

            Rules:
              - Use ONLY the tools listed below. Attribute each step to the server that ACTUALLY
                provides that tool — do not misfile a tool under the wrong server (e.g. convert
                belongs to the currency server; add/subtract/multiply to the calculator server).
              - Break the goal into the smallest sensible steps. Independent steps share no inputs
                (they could run in parallel); dependent steps list their prerequisites in "inputs".
              - If the goal asks to CHOOSE among options (the cheapest / best / lowest / highest X),
                enumerate the candidate options explicitly and build one parallel branch per
                candidate, each computed the SAME way; end with ONE compare/select node that takes
                ALL the candidates' results as inputs. Never select from a single value, and do not
                collapse the comparison to one arbitrary option.
              - Keep units consistent: convert amounts to a common currency before combining them;
                only add or subtract values already in the same currency; apply a fee or tax as its
                own explicit step. Never multiply a money amount by a percentage.
              - The graph MUST be acyclic, and every id in any "inputs" must be another node's id.
              - End with the step(s) that produce the user's final answer.

            Respond with ONLY a JSON object — no prose, no markdown fences:
            {"goal":"<restate the goal>","nodes":[{"id":"...","specialist":"...","op":"...","summary":"...","inputs":["..."]}]}

            Available tools (discovered over MCP):
            """;

    private final ChatClient planner;
    private final ToolCatalog catalog;
    private final ObjectMapper objectMapper;

    public PlanService(ChatClient.Builder chatClientBuilder, ToolCatalog catalog, ObjectMapper objectMapper) {
        // No tools, no memory: planning is a single, self-contained call.
        this.planner = chatClientBuilder.build();
        this.catalog = catalog;
        this.objectMapper = objectMapper;
    }

    public PlanResult plan(String goal) {
        long startNanos = System.nanoTime();
        ChatResponse response = planner.prompt()
                .system(PLANNER_PROMPT + catalog.forPrompt())
                .user(goal)
                .call()
                .chatResponse();
        long wallClockMs = (System.nanoTime() - startNanos) / 1_000_000;

        String text = answerText(response);
        PlanGraph graph = parse(goal, text);
        validate(graph, text);
        log.info("planned '{}' -> {} nodes", goal, graph.nodes().size());
        return new PlanResult(graph, catalog.tools(), summarizeUsage(response, wallClockMs));
    }

    // --- parsing & validation ----------------------------------------------------------------

    private PlanGraph parse(String goal, String text) {
        int open = text.indexOf('{');
        int close = text.lastIndexOf('}');
        if (open < 0 || close <= open) {
            throw new PlanException("the planner did not return a JSON plan", text);
        }
        try {
            PlanGraph parsed = objectMapper.readValue(text.substring(open, close + 1), PlanGraph.class);
            // Trust our goal string over the model's restatement.
            return new PlanGraph(goal, parsed.nodes());
        } catch (JacksonException e) {
            throw new PlanException("could not parse the planner's JSON: " + e.getMessage(), text);
        }
    }

    /** Teaching + robustness: a model-designed graph can be malformed — reject it clearly. */
    private void validate(PlanGraph graph, String raw) {
        List<PlanGraph.PlanNode> nodes = graph.nodes();
        if (nodes == null || nodes.isEmpty()) {
            throw new PlanException("the plan has no steps", raw);
        }
        Set<String> ids = new HashSet<>();
        for (PlanGraph.PlanNode n : nodes) {
            if (n.id() == null || n.id().isBlank()) {
                throw new PlanException("a step is missing an id", raw);
            }
            if (!ids.add(n.id())) {
                throw new PlanException("duplicate step id: " + n.id(), raw);
            }
        }
        for (PlanGraph.PlanNode n : nodes) {
            for (String dep : n.inputs()) {
                if (!ids.contains(dep)) {
                    throw new PlanException("step '" + n.id() + "' depends on unknown step '" + dep + "'", raw);
                }
            }
        }
        requireAcyclic(nodes, raw);
    }

    /** Kahn's algorithm: if we can't topologically order every node, there's a cycle. */
    private void requireAcyclic(List<PlanGraph.PlanNode> nodes, String raw) {
        Map<String, Integer> indegree = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();
        for (PlanGraph.PlanNode n : nodes) {
            indegree.put(n.id(), n.inputs().size());
        }
        for (PlanGraph.PlanNode n : nodes) {
            for (String dep : n.inputs()) {
                dependents.computeIfAbsent(dep, k -> new ArrayList<>()).add(n.id());
            }
        }
        Deque<String> ready = new ArrayDeque<>();
        indegree.forEach((id, deg) -> {
            if (deg == 0) {
                ready.add(id);
            }
        });
        int processed = 0;
        while (!ready.isEmpty()) {
            String id = ready.poll();
            processed++;
            for (String d : dependents.getOrDefault(id, List.of())) {
                if (indegree.merge(d, -1, Integer::sum) == 0) {
                    ready.add(d);
                }
            }
        }
        if (processed != nodes.size()) {
            throw new PlanException("the plan has a cycle (cannot be ordered)", raw);
        }
    }

    // --- usage -------------------------------------------------------------------------------

    private static RunUsage summarizeUsage(ChatResponse response, long wallClockMs) {
        Integer prompt = null, completion = null, total = null;
        String model = null;
        if (response != null && response.getMetadata() != null) {
            model = response.getMetadata().getModel();
            Usage u = response.getMetadata().getUsage();
            if (u != null) {
                prompt = u.getPromptTokens();
                completion = u.getCompletionTokens();
                total = u.getTotalTokens();
            }
        }
        double cost = Pricing.estimateUsd(model, prompt == null ? 0 : prompt, completion == null ? 0 : completion);
        return new RunUsage(prompt, completion, total, wallClockMs, cost, model,
                "one planning call (no execution); " + Pricing.note(model));
    }

    private static String answerText(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        String text = response.getResult().getOutput().getText();
        return text == null ? "" : text;
    }
}
