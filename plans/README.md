# Spring AI Agent — Learning Build

> **Handoff spec for a Claude Code session.** This is a *learning* project, not a
> production deliverable. The human is using it to internalize how agentic systems work,
> one concept at a time.

**Build it in PHASES, in order. Do not jump ahead.** Each phase is a self-contained
lesson. Finish, run, and let the human inspect a phase before starting the next.
The point is comprehension, not feature completeness — simplicity in early phases
is a feature, not a gap to fill.

---

## Phase index

Reasoning concepts come before delivery concepts. Build understanding while the wiring is
still simple; debugging a loop is far easier synchronously than through async + streaming.

| Phase | Title | Kind | File |
|-------|-------|------|------|
| 0 | Sync loop, ONE MCP, step visibility | reasoning (foundation) | [phase-0-sync-loop.md](phase-0-sync-loop.md) |
| 1 | Add 2nd MCP + a dependent multi-step task | reasoning (the real loop) | [phase-1-second-mcp.md](phase-1-second-mcp.md) |
| 2 | Failure & recovery | reasoning (underrated; do early) | [phase-2-failure-recovery.md](phase-2-failure-recovery.md) |
| 3 | Memory / multi-turn | reasoning | [phase-3-memory.md](phase-3-memory.md) |
| 4 | Async (202 + runId + poll) | delivery | [phase-4-async.md](phase-4-async.md) |
| 5 | Streaming the loop live (SSE) | delivery (the "watch it think" goal) | [phase-5-streaming.md](phase-5-streaming.md) |
| 6 | (frontier, optional) "Watch it Plan" — decompose a goal into a plan graph (no execution) | reasoning | [phase-6-watch-it-plan.md](phase-6-watch-it-plan.md) |

A through-line across all phases: **step visibility** (the "handrail"). Build the capture
hook once in Phase 0; every later phase logs, persists, or streams the *same* step data.

---

## Operating rules for the whole project

- **Maven**, multi-module.
- **Local git only.** `git init` in the project root. Commit after each phase with a
  clear message (e.g. `phase 0: sync loop + step logging`). No remotes, no push.
- **Local LLM via Ollama** is the base model (no API keys, no cost).
- **Verify versions against live docs.** Spring AI artifact IDs and config property keys
  shift between releases. Before writing dependencies/config, check the *current* Spring AI
  reference docs for: the Ollama starter, the MCP **client** starter (streamable-HTTP),
  and the MCP **server** starter (streamable-HTTP / WebMVC). Do NOT trust pinned snippets
  in this doc — they are illustrative, not authoritative.
- After each phase, briefly tell the human: what was built, how to run it, what to look at
  to *see the concept working*, and what the next phase will add.
- Keep dependencies minimal. Add things only when the phase needs them.

---

## Critical setup note: tool-capable Ollama model

The entire project is about the agent **calling MCP tools**. Many local models do tool-calling
poorly or not at all. If the chosen model can't call tools, the demo silently fails (no tools
fire, the model just answers from memory).

- Use a **tool-capable** model (e.g. a recent Llama 3.1+ or Qwen 2.5-class model — confirm
  current tool-supporting tags on the Ollama model page).
- Make the model name a **config property**, not hardcoded, so the human can swap it.
- Ollama default endpoint: `http://localhost:11434`.

A *deliberate* later lesson: try a weaker/non-tool model and watch the loop fail. That teaches
how much the model itself drives agent reliability. (Not a phase — an experiment the human can run.)

---

## Target architecture (reached gradually, not all at once)

```
spring-ai-agent-by-example/     parent POM (multi-module, local git)
│
├── mcp-server-currency/        Spring Boot — streamable-HTTP MCP
│     └── convert(), listRates()    (stands in for a real dynamic MCP)
│
├── mcp-server-calculator/      Spring Boot — streamable-HTTP MCP   (added Phase 1)
│     └── add(), maybe subtract()/multiply()
│
├── mcp-server-feestax/         Spring Boot — streamable-HTTP MCP   (added Phase 6)
│     └── transactionFee(), taxRate()
│
└── agent/                      Spring Boot — the orchestrator
      ├── Ollama ChatClient
      ├── MCP client → connects to all three mcp servers over streamable HTTP
      ├── the agent loop (Spring AI runs the tool-calling loop internally)
      └── REST endpoint(s)
```

Key separations (keep these throughout):
- The **agent** is separate from the **MCP servers**. Servers expose tools; the agent consumes them.
- The agent reaches tools **only over MCP/HTTP**, never by importing the server's Java classes.
- Agent state (when introduced) lives **outside** the request — not in instance fields.

---

## What NOT to do (applies to every phase)

- Don't put the agent loop *inside* an MCP server. Servers stay passive tool providers.
- Don't reach tools via Java imports — always over MCP/HTTP.
- Don't build async/streaming before the reasoning phases (0–3) are understood.
- Don't add a real message broker, database, auth, or tracing tooling — out of scope for
  learning. (Real observability tooling — Micrometer/OTel/Langfuse — is a *separate* later
  branch the human is intentionally skipping; their "observability" means a live human view,
  delivered by Phase 5 SSE.)
- Don't hardcode the model name or server URLs — use config.

---

## First action for the Claude Code session

1. Confirm Ollama is installed and a **tool-capable** model is pulled; surface the exact model
   tag being used.
2. Verify current Spring AI artifact IDs + config keys against live docs (Ollama starter,
   MCP client streamable-HTTP, MCP server streamable-HTTP).
3. Scaffold the multi-module Maven project + `git init`, then implement **Phase 0 only**.
4. Stop, run it, show the human the `steps[]` output, and wait before Phase 1.
