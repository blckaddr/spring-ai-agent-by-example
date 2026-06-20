# Chapter 1 — Setting the Stage

## What we wanted to learn

Before any code: what *is* the thing we're building, and how do we build it so we actually
understand it rather than just end up with a working pile?

The answer we committed to was a **learning ladder** — build in phases, in order, and don't move
on until the current rung makes sense:

| Phase | The concept |
|------:|-------------|
| 0 | See the tool-calling loop happen at all |
| 1 | A task that genuinely needs *multiple, dependent* tool calls |
| 2 | What happens when a tool *fails* |
| 3 | Memory across turns |
| 4 | Running long tasks detached (kick off, poll later) |
| 5 | Watching the agent think, live |

Reasoning concepts come first on purpose. A tool-calling loop is hard enough to reason about
when it's synchronous and simple; bolting on async and streaming before you understand the loop
just hides the loop behind more machinery.

We also set some rules to keep the lesson clean:

- The **agent** and the **tools** are separate programs. The agent reaches tools only over a
  protocol (MCP, over HTTP), never by calling their code directly. This mirrors how real agent
  systems are deployed — and it makes "the agent orchestrates independent services" literally
  true, not a metaphor.
- Run everything **locally** with a free model (via [Ollama](https://ollama.com)). No API keys,
  no bill, no excuse not to experiment.
- The model name is **configuration**, never hardcoded — because (foreshadowing) *which model
  you use turns out to matter enormously.*

## What we built (before any logic): a decision

The agent is built on [Spring AI](https://spring.io/projects/spring-ai), the Spring framework's
LLM toolkit. The very first real choice had nothing to do with agents and everything to do with
versions — and it's worth telling, because it's the kind of thing that silently wrecks a project.

Spring AI's version line is welded to Spring Boot's major version:

| Spring AI | Spring Boot |
|----------:|-------------|
| 1.1.x | 3.x |
| 2.0.x | 4.x |

Here's the trap: by mid-2026, if you let your build tool grab the "latest" Spring AI, you get
**2.0**, which silently drags in **Spring Boot 4**. If your project assumed Boot 3, things break
in confusing ways. So "use the latest" is not a safe default — the version has to be *pinned*
deliberately.

We weighed the two honestly:

- **The mature path** (Spring AI 1.1 + Boot 3) — more tutorials, more Stack Overflow answers,
  fewer surprises. Comfortable for learning.
- **The current path** (Spring AI 2.0 + Boot 4) — today's APIs, fewer worked examples, occasional
  "you're the first to hit this" moments.

We chose the **current** stack (Spring AI 2.0.0 + Boot 4.1.0, Java 21) — the point was to learn
today's tools, not last year's. We wrote the reasoning down as a decision record so future-us
wouldn't re-litigate it, and noted the fallback in case the newness got in the way of *learning*
(it didn't).

> 💡 **Takeaway.** "Latest" is not a version strategy. When a framework couples its version to a
> bigger framework's major release, pin both on purpose and write down why. We verified every
> artifact actually existed in the public repository before committing to it — trust, but verify.

## What we set up

Three small programs, each a normal Spring Boot app:

```
agent              (:8080)   the orchestrator — talks to the model, calls tools, exposes a REST API
mcp-server-currency (:8081)  a tool server — knows how to convert currencies
mcp-server-calculator (:8082) a tool server — knows how to add/subtract/multiply   (added in Ch.3)
```

Plus Ollama running a local model. We confirmed a **tool-capable** model was available —
`llama3.1:8b` — because here's a thing nobody warns you about: *many local models can't call
tools at all*, and when they can't, the agent doesn't error. It just quietly answers from memory
as if the tools weren't there. A silent failure is the worst kind, so we checked up front.

## What it taught us

Nothing about agents yet — and that's the lesson. Two of the highest-leverage decisions
(which framework version, which model) happen *before* you write a single line of agent logic,
and both fail *silently* if you get them wrong. Getting the boring foundation right is what makes
the interesting parts debuggable later.

Next: the smallest agent that can possibly work — and the one habit that made every later phase
legible.

→ [Chapter 2 — Seeing the Loop](02-seeing-the-loop.md)
