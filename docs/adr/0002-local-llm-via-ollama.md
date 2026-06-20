# ADR-0002 — Local LLM via Ollama with a tool-capable model

- **Status:** Accepted
- **Date:** 2026-06-20

## Context

The project needs an LLM to drive the agent loop. The whole point is the agent **calling MCP
tools**, so the model must support tool/function calling. Many local models do tool-calling
poorly or not at all, and when they can't, the demo *silently* fails — no tools fire and the
model just answers from memory, which looks like success but teaches nothing.

## Decision

Use a **local LLM via Ollama** (`http://localhost:11434`), no API keys, no cost. The model
must be **tool-capable** (recent Llama 3.1+ / Qwen 2.5-class — confirm the current
tool-supporting tag on the Ollama model page). The model name is a **config property**, never
hardcoded, so it can be swapped. The exact tag in use is recorded in
[`NOTES.md`](../../NOTES.md).

## Consequences

- **Good:** free, offline, fast iteration; swappable model; reinforces how much the model
  itself drives agent reliability.
- **Bad / cost:** local models are weaker/less reliable than hosted frontier models; tool
  calling may be flaky. That unreliability is itself a *deliberate lesson*.
- **Follow-ups:** a non-phase experiment — run a weaker/non-tool model and watch the loop fail,
  to internalize the model's role. Record observations in `NOTES.md`.

## Alternatives considered

- **Hosted API (Anthropic/OpenAI/etc.)** — costs money + keys; out of scope for a free local
  learning build. May revisit if local tool-calling proves too unreliable to learn from.
