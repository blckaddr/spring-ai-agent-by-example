# Phase 5 — Streaming the loop live (SSE)

**Kind:** delivery (the "watch it think" goal)

> See [README.md](README.md) for operating rules, architecture, and the full phase ladder.
> Prerequisite: [Phase 4](phase-4-async.md) understood and running.

## Learning goal

The human's actual definition of "observability" — a person watching the agent work **live**,
step by step, as it happens. Same step data as always, now *pushed* as each step fires instead
of collected at the end.

## Build

- An SSE endpoint that streams step events (`thinking… → called convert(…) → got 79.00 → …`)
  to a client as the loop runs.
- This is the hard/instructive part: Spring AI runs the tool loop internally and hands back a
  final answer, so prying out *intermediate* steps live is the lesson. Use the capture hook to
  emit events onto a stream/sink, or use the streaming API and surface tool-call events.
- A minimal HTML page (single file) that opens the SSE stream and appends steps live is enough
  to *see* it. No framework needed.

## Done when

The human watches steps appear one by one in real time as the agent works.

## Next

[Phase 6](phase-6-multi-agent.md) — optional frontier: planning / multi-agent. Build only if
asked.
