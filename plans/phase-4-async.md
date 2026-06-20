# Phase 4 — Async (202 + runId + poll)

**Kind:** delivery

> See [README.md](README.md) for operating rules, architecture, and the full phase ladder.
> Prerequisite: reasoning phases [0](phase-0-sync-loop.md)–[3](phase-3-memory.md) understood.

## Learning goal

Detached execution and run state — *delivery*, not reasoning. Motivated by "real loops can
run long and can't hold an HTTP connection."

## Build

- `POST /agent/run` now returns **202 + runId** immediately; the loop runs detached
  (background executor is fine — no message broker needed for learning).
- A **run store** (status: queued/running/done/failed) holding the result AND the `steps[]`
  captured by the Phase-0 hook (now *persisted*, not just returned).
- `GET /agent/run/{runId}` to poll status + result + steps.
- Add a loop **safety cap** (max steps / max wall-clock) and basic idempotency by event id.

## Note

Keep the synchronous endpoint from earlier phases available too (the human asked for both
styles). Async is an addition, not a replacement.

## Done when

A run can be kicked off, polled, and its final answer + steps retrieved after completion —
surviving the fact that the request already returned.

## Next

[Phase 5](phase-5-streaming.md) pushes the same step data live over SSE — the "watch it
think" goal.
