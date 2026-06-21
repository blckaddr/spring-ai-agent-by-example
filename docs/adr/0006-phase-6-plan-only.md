# ADR-0006 — Phase 6 is "plan-only" (decompose & visualize), not multi-agent execution

- **Status:** Accepted
- **Date:** 2026-06-21

## Context

Phase 6 is the optional frontier — the rung beyond a single reactive tool-calling loop. Two
shapes were on the table:

1. **Multi-agent execution** — an orchestrator that emits a plan and then *delegates* sub-tasks to
   specialist sub-agents (agents calling agents), executing the work in parallel.
2. **Plan-then-execute, visualized** — the model decomposes a goal into an explicit plan graph,
   which we *show* but do not run.

The project's purpose is comprehension (ADR-0004), and the domain is small (currency + calculator,
later + fees/tax). A first draft built option 1 (orchestrator + currency/calc sub-agents executing
in parallel). In practice each "sub-agent" was an LLM wrapping a single tool call — pure overhead —
and a two/three-tool domain never actually needs delegation to get the right answer. The
interesting, genuinely-new concept on this rung is **decomposition**, not orchestration plumbing.

## Decision

Phase 6 ("Watch it Plan") makes **one tool-less model call** that decomposes a goal into a
validated dependency graph (`PlanService` → `PlanGraph`), renders it in the browser (`/plan`), and
**executes nothing**. The planner plans against the tools it discovers over MCP (`ToolCatalog`) but
never calls them. Planner ≠ executor: actually running the graph is explicitly out of scope (the
next rung).

## Consequences

- **Good:** isolates decomposition as the single new idea; honest and cheap (one call, zero tool
  executions); the graph makes the model's reasoning — and its flaws — inspectable; no fake
  "intelligence" in thin sub-agents.
- **Bad / cost:** does not demonstrate execution, delegation, or parallelism; "cheapest-X" style
  goals are only *planned*, not answered. Bigger goals strain a small model (dropped node refs →
  the validator rejects them).
- **Follow-ups:** a future phase could execute the validated DAG with a dependency-driven runtime
  (model as planner, code as executor). See book Ch 10.

## Alternatives considered

- **Multi-agent orchestration + executing sub-agents** — built first, then dropped: sub-agent LLMs
  were thin one-tool wrappers (overhead), and the domain doesn't need delegation. It reinforced
  existing lessons rather than teaching a new one.
- **Plan *and* execute now** — more moving parts (executor, result threading, error recovery across
  the boundary) for little additional learning at this scale; deferred.
