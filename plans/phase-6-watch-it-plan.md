# Phase 6 — (Optional frontier) Watch it Plan

**Kind:** reasoning (frontier · optional)

> See [README.md](README.md) for operating rules, architecture, and the full phase ladder.
> Prerequisite: phases [0](phase-0-sync-loop.md)–[5](phase-5-streaming.md) built and understood.
>
> **Build only if explicitly asked.** Phases 0–5 are the core lesson; this isolates the one
> concept beyond them.

## Learning goal

The single genuinely-new concept beyond the reactive loop of phases 0–5: **decomposition
(plan-then-execute).** Given a goal, the model produces an explicit **plan graph** — sub-tasks
(nodes) and their data dependencies (edges) — *before* anything runs. We **visualize the plan and
do not execute it.** The plan graph is the artifact.

This deliberately drops the earlier "team of sub-agents that execute" idea. That design had each
sub-task spawn its own LLM which then called a single tool — pure overhead, and it muddied the
lesson. Here the honest claim stands alone:

> A model is genuinely good at turning a fuzzy goal into a **structured, dependency-aware plan**
> over the available tools. That structuring is the LLM's contribution — and you can *see* it.

What the LLM does (and doesn't):
- **Does:** natural-language understanding (extract amounts, intent, target), pick which tools to
  use, name the steps, and wire the dependencies into a DAG.
- **Doesn't:** execute. No tool is ever called in this phase; the planner only needs to *discover*
  that the tools exist.

## Why a richer domain (and a 3rd server)

With only currency + calculator, a plan is a shallow fan-in. To make decomposition worth seeing,
add a third specialist: a **fees/tax** MCP server (`mcp-server-feestax`, :8083) exposing
`transactionFee(amount, currency)` and `taxRate(currency)` (fixed demo values, like the currency
server's rates). Now a goal like:

> *"I hold 100 USD, 50 EUR, 5000 JPY, 200 CAD and 80 CHF. What's the cheapest currency to
> consolidate everything into, once transaction fees and tax are included?"*

forces a real multi-layer graph: convert across currencies, look up fees + tax, combine, compare.

**The tools are never executed**, so the third server is cheap — its only job is to appear in the
catalog the planner reads. (It keeps the hard rule intact: tools live in MCP servers, discovered
over MCP — never hardcoded in the agent. See [ADR-0003](../docs/adr/0003-tools-over-mcp-only.md).)

## Build

- **`mcp-server-feestax`** (:8083) — a third passive MCP server; `transactionFee` + `taxRate`
  with demo values. No agent loop (like the others).
- **`ToolCatalog`** (agent) — discovers every connected server's tools (name, description, params)
  over MCP at startup, and renders them for the planner prompt. Honest discovery, not a hardcoded list.
- **`PlanService`** (agent) — ONE tool-less model call. The planner returns JSON
  `{goal, nodes:[{id, specialist, op, summary, inputs[]}]}`. We parse it (first `{`…last `}`,
  Jackson) and **validate**: unique ids, every `inputs` id exists (no dangling refs), and the graph
  is **acyclic** (Kahn's algorithm). Invalid → `PlanException` carrying the raw model text.
- **`POST /agent/plan`** (`PlanController`) — goal in, `{graph, catalog, usage}` out. A normal
  request/response (planning is one call — no async, no SSE). 422 + raw text on an invalid plan.
- **`/plan` page** (`plan.html`, served via `WebConfig`) — renders the graph with **Mermaid**
  (vendored locally at `static/vendor/mermaid.min.js` for offline use), as a flowchart with one
  **subgraph per specialist**, colored by specialist. Same chat-style shell as `/` (header, scroll
  area, input pinned at the bottom). Single-agent endpoints/pages are untouched.

The Phase-0 step-capture handrail is NOT used here — there are no tool steps, only a plan. (The
`lane`/streaming machinery from the earlier team draft was reverted.)

## Watch for / discuss

- **Where the intelligence is** — entirely in the plan. Execution would be deterministic; the model
  doesn't do arithmetic here, it designs the workflow.
- **Plan quality is inspectable.** The graph makes the model's *interpretation* visible — including
  when it's imperfect (e.g. consolidating into one reference currency instead of comparing several,
  or an odd `multiply` of value × rate × fee). Reading the graph beats trusting a prose answer —
  the Phase-2 lesson, one level up.
- **Non-determinism.** Re-running the same goal yields different (valid) graphs. That's planning.
- **The boundary to execution.** A natural (out-of-scope) next step: run the validated DAG with a
  dependency-driven executor (futures keyed on `inputs`), tools called directly — LLM as *planner*,
  code as *runtime*.

## Done when

The consolidation goal returns a **valid** plan graph (acyclic, all refs resolve) spanning the
three specialists + an orchestrator decision node, and `/plan` renders it as a clean,
specialist-grouped diagram — **and** the single-agent chat at `/` still works unchanged.

## Honest caveat (state it in the book)

For this domain a single agent already *answers* the question; this phase doesn't try to. Its point
is to make **decomposition** visible and honest: one model call, one graph, no theatrics. That's the
whole, true lesson — and it matches Anthropic's "start simple."

## Next

End of the ladder. Beyond here: actually executing the plan graph, re-planning loops, evaluator
agents — real agent-systems territory, out of scope for this learning build.
