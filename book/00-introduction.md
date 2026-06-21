# Introduction

Most "build an AI agent" tutorials hand you a finished demo: paste the code, run it, watch it work,
feel clever. Then you change one thing, it breaks in a way the tutorial never mentioned, and you
realize you were shown a *magic trick*, not a machine.

This book is the opposite. It's the diary of building one small agent **slowly** — one concept at a
time — and *watching what the loop actually does*, including the parts that go wrong. The parts that
go wrong are where the understanding is.

The agent's job is deliberately boring: convert some currencies, add some numbers, by calling tools
that live in separate little servers. Boring is the point. With the domain out of the way, the
*machinery* stays in plain view — how a model decides to call a tool, how results flow back, how a
multi-step plan holds together or quietly falls apart.

By the end you'll have watched an agent reason across tools, recover from a failure, remember a
conversation, report its own cost, run detached, stream its thinking live, and finally decompose a
goal into a plan. More to the point, you'll have seen — at each step — *where the hard parts
actually are.* They're rarely where the tutorials point: the model's reliability ceiling, a clear
error message as a design decision, memory faithfully carrying mistakes forward, the loop as the
cost multiplier, a bigger toolset making a small model *worse.*

A few things to know going in:

- **It's a ladder.** Each chapter is one rung, in order — reasoning concepts first, while the wiring
  is simple enough to debug; delivery concepts later. Don't skip; each rung leans on the one below.
- **Every chapter follows the same beat:** *What we wanted to learn → What we built → What actually
  happened → What it taught us.* The "what actually happened" parts quote real output — wrong
  answers included, because those taught us the most.
- **You can just read it.** The story stands alone. But each chapter ends with a **"The code"** list
  (jump straight to the real classes) and a **"Try it yourself"** box (run it against a live agent)
  for when you want your hands dirty. Setup is in the [README](../README.md) — it's all local and
  free, no API keys.

Ready? Fittingly, the first thing to get right has nothing to do with agents — and everything to do
with not getting silently wrecked before you write a line of logic.

---

→ [Chapter 1 — Setting the Stage](01-setting-the-stage.md)
