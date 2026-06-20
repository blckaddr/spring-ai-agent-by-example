# Building an Agent, One Concept at a Time

*A learning journal — how a Spring AI agent that calls tools actually works, discovered by
building it in small, honest steps.*

---

## Why this book exists

Most "AI agent" tutorials hand you a finished, working demo and the illusion that it always
works. This is the opposite. It's the diary of building one agent slowly — a phase at a time —
and *watching* what the loop really does, including the parts that fail. The goal was never a
product. It was understanding.

The agent's job is deliberately mundane: convert currencies and add numbers, by calling tools
that live in separate little servers. The mundaneness is the point — it keeps the *machinery*
(how a model decides to call a tool, how results flow back, how multi-step plans hold together
or fall apart) in plain view.

If you read it start to finish, you should come away understanding not just *how* to wire up an
agent, but *where the hard parts actually are* — which is rarely where the tutorials say.

## Who it's for

Anyone curious how LLM agents work under the hood — developers especially, but the narrative
parts assume only that you've used a chatbot before. Code and config live in the repo; the book
quotes the interesting bits and explains them, but you don't need to run anything to follow the
story.

## How it's organized

Each chapter is one rung of a "learning ladder" — reasoning concepts first (while the wiring is
simple enough to debug), delivery concepts later. Every chapter follows the same beat:

> **What we wanted to learn → What we built → What actually happened → What it taught us.**

The "what actually happened" sections quote real output — including the wrong answers, because
the wrong answers taught us the most.

## Chapters

1. [Setting the Stage](01-setting-the-stage.md) — the goal, the ladder, and a versioning trap
   that nearly bit us before we wrote a line of logic.
2. [Seeing the Loop](02-seeing-the-loop.md) *(Phase 0)* — the smallest possible agent, and the
   one habit that made everything afterward legible.
3. [The Real Loop](03-the-real-loop.md) *(Phase 1)* — two tool servers, a task that *needs*
   several steps, and the moment the model's limits became impossible to ignore.
4. [A Better Brain](04-a-better-brain.md) *(experiment)* — we change nothing but the model, and
   a failure that no prompt could fix simply vanishes.
5. [When Tools Fail](05-when-tools-fail.md) *(Phase 2)* — we break the tools on purpose and learn
   that a good error message is an agent-design decision.
6. [Giving It Memory](06-giving-it-memory.md) *(Phase 3)* — the agent stops forgetting between
   requests, and we learn that memory faithfully carries forward mistakes too.
7. [What Does It Cost?](07-what-does-it-cost.md) *(Phase 3.5)* — we make tokens, time, and dollars
   observable, hit a wrong-layer dead end, and learn what's easy vs hard about cost.
8. [Letting It Run Detached](08-letting-it-run-detached.md) *(Phase 4)* — kick off, poll later; the
   run outlives the request, and unattended loops need guard rails.
9. [Watching It Think](09-watching-it-think.md) *(Phase 5)* — the finale: each step pushed live over
   SSE, with a tiny HTML page. The "watch it think" goal, achieved almost for free thanks to Ch 2.

## Running it while you read

The build is **cumulative** — the final `main` has every phase's features — so every chapter's
**"Try it yourself"** box works against a single running stack. See the
[root README](../README.md) to start it (Ollama + three services), then run each chapter's commands
as you go. To inspect the code as it was at a given phase, check out its tag (e.g.
`git checkout phase-2`); read the prose here on `main`.

## The companion materials

This book is the *story*. The repo also keeps:

- [`plans/`](../plans/) — the phase-by-phase build plan (what we intended to do).
- [`docs/adr/`](../docs/adr/) — the decisions we made and why (e.g. which framework versions).
- [`docs/architecture/`](../docs/architecture/) — how the pieces fit together.
- [`NOTES.md`](../NOTES.md) — the running lab notebook (versions, ports, raw observations).

You can read the book alone. The links are there when you want to go deeper.
