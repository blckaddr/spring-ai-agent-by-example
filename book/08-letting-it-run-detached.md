# Chapter 8 — Letting It Run Detached *(Phase 4)*

## What we wanted to learn

Every loop so far has been *synchronous*: the caller fires a request and waits, holding the HTTP
connection open, until the whole loop finishes. We saw runs take 20–30 seconds already. Real agent
runs can take minutes — and no browser, mobile app, or load balancer will happily hold a connection
that long. So this phase is about **detached execution**: kick off a run, get an id back
immediately, and poll for the result later.

This is the project's first *delivery* concern rather than a *reasoning* one. The agent gets no
smarter here; we change how its work is *delivered*.

## What we built

**Return early, run in the background.** A new endpoint `POST /agent/runs` creates a run, hands it
to a background executor, and returns **HTTP 202 Accepted with a runId** right away — without
waiting for the loop. `GET /agent/runs/{runId}` lets the caller poll.

**A run store.** An in-memory map of runs, each a small record:

```
runId · status (QUEUED → RUNNING → DONE/FAILED) · result · error · createdAt · finishedAt
```

The `result` is the *same* `AgentResponse` the sync endpoint returns — answer, `steps[]`, and the
`usage` block from Chapter 7. This is the Phase-0 handrail paying off again: the step/usage data we
built once is now simply **persisted** instead of returned inline. No new capture code.

**Safety caps**, because an unattended loop needs guard rails:
- **max wall-clock** — if a detached run overruns, it's marked `FAILED` with a timeout reason.
- **max steps** — a hard cap on tool calls per run, enforced by the same capture hook.

**Idempotency by event id** — an optional key so a retried request (network blip, at-least-once
delivery) returns the *same* run instead of starting a duplicate.

**The sync endpoint stays.** `POST /agent/run` still blocks and returns inline. Async is an
addition, not a replacement — both delivery styles coexist.

> A note on scope: the store is in-memory, like the chat memory in Chapter 6. Runs vanish on
> restart and don't span instances. That's the deliberate learning boundary; the seam to swap in
> Redis/JDBC (and a real queue for the executor) is exactly the `RunStore` component.

## What actually happened

Kicking off the multi-currency task asynchronously:

```
POST /agent/runs  →  202 Accepted   { "runId": "000e1e64-…", "status": "QUEUED" }

GET /agent/runs/000e1e64-…   (polling)
  QUEUED → RUNNING → RUNNING → … → DONE

final record:
  status: DONE   created: 15:12:26Z   finished: 15:12:45Z
  result.answer: "…the total … is approximately 147.09 GBP."
  result.steps:  7      result.usage: { totalTokens: 964, wallClockMs: 19615, … }
```

The request returned in milliseconds with a runId; the 19-second loop ran *after* the HTTP call had
already completed; and the full result — answer, every step, the cost — was waiting in the store
when we polled. **The run outlived the request.** That's the whole point.

Three more behaviors we confirmed:

- **The sync endpoint still works** unchanged — both styles live side by side.
- **Idempotency holds** — two `POST /agent/runs` with the same `eventId` returned the *same* runId;
  the second didn't start a duplicate run.
- **The safety cap bites.** We set `max-steps = 2` and asked for a 4-step task. The run came back
  `FAILED` with `error: "max steps (2) reached for this run"` — a clean, legible hard stop rather
  than a runaway loop. (The error is captured in the run record just like a tool failure; an
  unattended agent that can't stop itself is a bug, not a feature.)

## What it taught us

- **Detachment is a state-management problem, not a cleverness problem.** Once a run outlives its
  request, you need somewhere to *keep* its state and lifecycle — `QUEUED/RUNNING/DONE/FAILED` — and
  a way to address it (the runId). That little record *is* the feature.
- **The handrail compounds.** Because steps and usage were captured from Phase 0, "persist the
  result" was almost free — store the object we already had. Build the observability seam once, reap
  it every phase.
- **Unattended means guard rails are mandatory.** A synchronous caller can always give up by closing
  the tab. A detached loop can't be abandoned that way — so max-steps and max-wall-clock aren't
  niceties, they're the difference between a safe background job and a runaway one.
- **Idempotency is cheap insurance.** The moment delivery is asynchronous, retries happen; a one-line
  event-id check turns "duplicate runs on every network blip" into "exactly once."

The agent now reasons, orchestrates, recovers, remembers, reports its cost, and runs detached. But
polling still hides the *process* — you only see the result when it's all over. The original goal
was to *watch the agent think, live*. That's the finale.

→ *Chapter 9 — Watching It Think (Phase 5, upcoming)*
