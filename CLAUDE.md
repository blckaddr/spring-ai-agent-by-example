# CLAUDE.md

Guidance for Claude Code working in this repo. Read this first, then [`plans/README.md`](plans/README.md).

## What this is

A **learning** project: a Spring AI agent that calls MCP tools, built to teach how agentic
systems work — one concept per phase. **Comprehension is the goal, not feature completeness.**
Simplicity in early phases is a feature, not a gap to fill.

The full spec lives in [`plans/`](plans/) — an overview plus one file per phase. Build the
phases **in order, one at a time.** Finish, run, and let the human inspect a phase before
starting the next. Do not jump ahead or pre-build later-phase concerns.

## Hard rules (do not violate)

- **Phases are gated.** Implement only the current phase. After each: stop, run it, show the
  human the `steps[]` output / the thing that makes the concept visible, and wait.
- **Verify versions against live docs.** Spring AI artifact IDs and config property keys shift
  between releases. Before writing any dependency or config, check the *current* Spring AI
  reference docs (Ollama starter, MCP **client** streamable-HTTP, MCP **server**
  streamable-HTTP / WebMVC). The snippets in `plans/` are illustrative, NOT authoritative.
- **No hardcoding** the model name or server URLs — always config properties.
- **Agent ≠ MCP server.** Servers are passive tool providers. The agent reaches tools **only
  over MCP/HTTP**, never by importing a server's Java classes. Never put the agent loop inside
  an MCP server.
- **Agent state lives outside the request** — not in controller/instance fields.
- **Don't build ahead of the ladder.** No async/streaming before reasoning phases (0–3) are
  understood. No real broker, database, auth, or tracing/observability tooling (Micrometer/
  OTel/Langfuse) — explicitly out of scope. "Observability" here = the live human view
  delivered by Phase 5 SSE.
- **Keep dependencies minimal.** Add a dependency only when the current phase needs it.

## The through-line: step visibility

The "handrail" of the whole project is **step capture** — recording each tool call as
`{ step, tool, server, arguments, result, latencyMs }`. Build this hook **cleanly in Phase 0**;
every later phase reuses the *same* step data — logged (0), chained (1), error-annotated (2),
context-aware (3), persisted (4), then streamed live (5). Don't reinvent it per phase.

## Tooling & environment

- **Build:** Maven, multi-module. Parent POM + modules under the project root.
- **Git:** local only. `git init`, commit after each phase (e.g. `phase 0: sync loop + step
  logging`). No remotes, no push.
- **LLM:** local via **Ollama** (`http://localhost:11434`), no API keys.
- **Model:** must be **tool-capable** (recent Llama 3.1+ / Qwen 2.5-class — confirm current
  tool-supporting tag on the Ollama model page). A non-tool model makes the demo *silently*
  fail (no tools fire; the model answers from memory). This is the #1 footgun. Record the exact
  tag in use in [`NOTES.md`](NOTES.md).

## After each phase, tell the human

1. What was built. 2. How to run it. 3. What to look at to *see the concept working*.
4. What the next phase adds.

## Target architecture (reached gradually)

```
spring-ai-agent-by-example/   parent POM (multi-module, local git)
├── mcp-server-currency/      Spring Boot — streamable-HTTP MCP — convert(), listRates()
├── mcp-server-calculator/    Spring Boot — streamable-HTTP MCP — add() (+ subtract/multiply)   [Phase 1]
├── mcp-server-feestax/       Spring Boot — streamable-HTTP MCP — transactionFee(), taxRate()   [Phase 6]
└── agent/                    Spring Boot — Ollama ChatClient + MCP client (all three servers) + REST/SSE
```

## Git history & publishing

- The project is **born-named** `spring-ai-agent-by-example` — there is deliberately NO rename
  commit in history. Only that name should ever appear; don't reintroduce any earlier working name
  in code, docs, or commits.
- Already published to GitHub (`origin`); updating is a normal `git push`. No release ceremony.
- **No per-phase tags.** The book is read on `main` (cumulative build); each chapter links its key
  classes via its "The code" list. Any leftover `phase-*` tags (local and on the remote) are retired.

## Pointers

- Plan / phases: [`plans/README.md`](plans/README.md)
- Running state, decisions, env facts: [`NOTES.md`](NOTES.md)
- The shareable narrative: [`book/README.md`](book/README.md)
