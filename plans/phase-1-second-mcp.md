# Phase 1 — Second MCP server + a dependent multi-step task

**Kind:** reasoning (the real loop)

> See [README.md](README.md) for operating rules, architecture, and the full phase ladder.
> Prerequisite: [Phase 0](phase-0-sync-loop.md) understood and running.

## Learning goal

Multi-server tool aggregation, and a task that genuinely **requires a loop** (sequential,
dependent tool calls — not one round-trip).

## Build

- Add `mcp-server-calculator`: streamable-HTTP MCP exposing `add(...)` (optionally
  `subtract`, `multiply`).
- Agent's MCP client now connects to **both** servers; all tools merge into one toolset.
- Use **granular** tools (convert + add separately) so the *model* orchestrates — do NOT make
  a single coarse `convertAndSum` tool. The orchestration is the lesson.
- Target task: *"Add 100 USD, 50 EUR and 5000 JPY and give the total in GBP."*
  Expected loop: 3× `convert` → 1× `add`. `add` can't run until the converts return.

## Watch for / discuss with the human

- Tool **name collisions** across servers and how Spring AI disambiguates.
- The `steps[]` now showing a multi-step chain — the first real agentic loop.
- (Teaching aside) where intelligence should live: model-orchestrates (granular) vs
  server-does-it (coarse).

## Done when

The multi-currency-sum task completes and `steps[]` shows the full convert×3 → add chain,
with tools drawn from *both* servers.

## Next

[Phase 2](phase-2-failure-recovery.md) probes what the loop does when a tool throws.
