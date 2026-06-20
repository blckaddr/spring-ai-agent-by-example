# Phase 2 — Failure & recovery

**Kind:** reasoning (underrated; do early)

> See [README.md](README.md) for operating rules, architecture, and the full phase ladder.
> Prerequisite: [Phase 1](phase-1-second-mcp.md) understood and running.

## Learning goal

What the loop does when a tool **throws**. This is the most important behavior for an
unattended agent and is nearly free to set up.

## Build / probe (little new code)

- Feed inputs that make a tool fail: unsupported currency (`convert(…, "ZZZ")`), or have the
  calculator reject bad input. Ensure the MCP tool surfaces a clear error.
- Observe via `steps[]`/logs: does the model retry? pick another tool? apologize and stop?
  hallucinate a number instead of using the tool?
- Add only what's needed to make failures legible (capture the error in the step record).

## Done when

The human can trigger a tool failure and *read from the steps* exactly how the loop responded.
The lesson is the observed behavior, not a feature.

## Next

[Phase 3](phase-3-memory.md) adds conversation state / multi-turn memory.
