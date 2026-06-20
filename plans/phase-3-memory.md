# Phase 3 — Memory / multi-turn

**Kind:** reasoning

> See [README.md](README.md) for operating rules, architecture, and the full phase ladder.
> Prerequisite: [Phase 2](phase-2-failure-recovery.md) understood and running.

## Learning goal

Conversation state. The agent goes from stateless function to something that remembers.

## Build

- Add chat memory (Spring AI memory advisor) keyed per session/conversation id.
- Endpoint accepts a `sessionId`; follow-ups like *"now convert that total to yen"* resolve
  "that" from prior context.
- Keep memory store simple (in-memory is fine for learning); note that production would
  externalize it (Redis/JDBC).

## Done when

A second request referencing the first works, and `steps[]` shows prior context being used.

## Next

[Phase 4](phase-4-async.md) detaches execution: 202 + runId + poll. This is where the
project shifts from *reasoning* concepts to *delivery* concepts.
