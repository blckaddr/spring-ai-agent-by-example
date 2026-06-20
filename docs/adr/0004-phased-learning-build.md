# ADR-0004 — Build in gated learning phases

- **Status:** Accepted
- **Date:** 2026-06-20

## Context

This is a comprehension project, not a delivery project. The risk is rushing to a
feature-complete agent (async, streaming, memory all at once) and never understanding the loop
underneath. Debugging a tool-calling loop is far easier synchronously than through async +
streaming.

## Decision

Build in **ordered, gated phases** (see [`plans/`](../../plans/)). Reasoning concepts
(phases 0–3) come before delivery concepts (4–5). Implement **one phase at a time**; after each,
stop, run it, show the human the step output that makes the concept visible, and wait before
the next. A single through-line — **step visibility** — is built once in Phase 0 and reused
(logged → chained → persisted → streamed) by every later phase.

## Consequences

- **Good:** each concept is isolated and inspectable; simplicity early is a feature; the step
  hook is built once, not reinvented.
- **Bad / cost:** slower than building everything at once; requires discipline not to
  pre-build later-phase concerns.
- **Follow-ups:** keep `NOTES.md` phase-status table current; commit after each phase.

## Alternatives considered

- **Build the full agent, then refactor for understanding** — defeats the purpose; the loop
  stays a black box.
