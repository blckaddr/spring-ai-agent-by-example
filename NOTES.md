# NOTES — running state & decisions

Scratchpad for facts that change as the build proceeds. Keep it current; it's the memory
between sessions. (Contract & rules live in [`CLAUDE.md`](CLAUDE.md); the plan in [`plans/`](plans/).)

## Environment (fill in once verified)

| Thing | Value | Verified? |
|-------|-------|-----------|
| Ollama installed | yes — client `0.21.2` | ☑ (2026-06-20) |
| Ollama endpoint | `http://localhost:11434` | ☑ |
| Ollama server running? | **NO at check time** — start with `ollama serve` or the desktop app before running the agent | ☐ |
| Model tag (default, tool-capable) | **`llama3.1:8b`** (on disk) | ☑ |
| Models for failure experiments | `qwen2.5:3b` (weak), `qwen2.5:0.5b` (very weak) — both on disk; `llama3` on disk is NOT reliably tool-capable, don't use | ☑ |
| Spring Boot version | `4.1.0` (Spring AI 2.0.0 resolves to Boot 4.1.0) | ☑ (2026-06-20) |
| Spring AI version (BOM) | `2.0.0` (2.0.x line — pairs with Boot 4.x) — see [ADR-0005](docs/adr/0005-spring-ai-2-on-boot-4.md) | ☑ (2026-06-20) |
| Java version | `21.0.11` (Homebrew OpenJDK) | ☑ |
| Maven version | `3.9.15` | ☑ |

## Spring AI artifact IDs / config keys — VERIFIED for 2.0.0 line (2026-06-20)

> All artifacts confirmed present on Maven Central at 2.0.0. groupId is `org.springframework.ai`
> for all. Versions are BOM-managed — do NOT put `<version>` on the starters. Use the
> **unversioned** (= 2.0) docs tree, NOT `/reference/1.1/`.

**BOM** (in parent `<dependencyManagement>`):
- `org.springframework.ai:spring-ai-bom:2.0.0` (`<type>pom</type>`, `<scope>import</scope>`)

**Ollama starter** (agent module):
- artifact: `spring-ai-starter-model-ollama`
- `spring.ai.ollama.base-url` (default `http://localhost:11434`)
- `spring.ai.ollama.chat.model` (default `mistral` → set to `llama3.1:8b`)
- `spring.ai.ollama.chat.temperature` (2.0: **no `.options.` prefix**; default `0.8`)
- enablement: `spring.ai.model.chat=ollama` (default on; `none` to disable). `spring.ai.ollama.chat.enabled` is deprecated.
- tool calling: no special flag — register tool callbacks on the ChatClient. Needs Ollama ≥ 0.2.8 + a tool-capable model.

**MCP client starter** (agent module — connects OUT over streamable-HTTP):
- artifact: `spring-ai-starter-mcp-client` (sync/WebMVC). (`-webflux` variant = reactive.)
- `spring.ai.mcp.client.type=SYNC`
- `spring.ai.mcp.client.toolcallback.enabled=true` (surfaces remote tools as ToolCallbacks)
- `spring.ai.mcp.client.streamable-http.connections.<id>.url` (e.g. `http://localhost:8081`)
- `spring.ai.mcp.client.streamable-http.connections.<id>.endpoint` (optional, default `/mcp`)
- bean to inject: `SyncMcpToolCallbackProvider` → `ChatClient...defaultTools(provider.getToolCallbacks())` (or `.tools(...)` per request)

**MCP server starter** (each mcp-server-* module — exposes tools over streamable-HTTP, WebMVC/sync):
- artifact: `spring-ai-starter-mcp-server-webmvc`
- `spring.ai.mcp.server.protocol=STREAMABLE` (vs SSE)
- `spring.ai.mcp.server.type=SYNC`
- `spring.ai.mcp.server.name`, `spring.ai.mcp.server.version`
- `spring.ai.mcp.server.streamable-http.mcp-endpoint` (default `/mcp`)
- tools: `@Tool(description=...)` (package `org.springframework.ai.tool.annotation.Tool`) on a `@Service`/`@Component` method, registered via a `ToolCallbackProvider` bean built with `MethodToolCallbackProvider.builder().toolObjects(svc).build()`. Auto-config picks up `ToolCallbackProvider`/`ToolCallback` beans automatically.
- NOTE (2.0): MCP annotation packages moved to `org.springframework.ai.mcp.annotation` — relevant only if using `@McpTool`-style annotations; the `@Tool`/`MethodToolCallbackProvider` path above is unaffected.

## Phase status

| Phase | Status | Commit | Notes |
|-------|--------|--------|-------|
| 0 — sync loop, 1 MCP, step visibility | ☑ done & verified | (see git log) | "convert 100 USD to EUR" → 92.0; steps[] + console log show the convert call. Ports: agent 8080, currency 8081. |
| 1 — 2nd MCP + dependent multi-step | ☑ done & verified (structure) | (see git log) | calculator server (:8082, 3 tools) added; agent aggregates both; convert×3→add chain + per-server attribution work. Model threads tool outputs unreliably — see observations. |
| 2 — failure & recovery | ☐ not started | — | |
| 3 — memory / multi-turn | ☐ not started | — | |
| 4 — async (202 + runId + poll) | ☐ not started | — | |
| 5 — streaming live (SSE) | ☐ not started | — | |
| 6 — multi-agent (optional) | ☐ not started | — | build only if asked |

## Decisions log

- _(date)_ Plan split into `plans/` overview + per-phase files; added `CLAUDE.md` + `NOTES.md`.

## Open questions / things to confirm with the human

- Exact model tag the human wants to run as the default.
- _(add as they come up)_

## Observed agent behavior (lessons captured while building)

> Especially Phase 2: how the loop reacts to tool failures. Also the "weak/non-tool model"
> experiment from the README — record what actually happens.

- **Phase 1 (llama3.1:8b), multi-currency-sum task** — the model's behavior splits cleanly into
  three traits, each surfaced only by `steps[]`:
  - *Orchestration order:* with NO system prompt it called `add` prematurely (on raw cross-currency
    amounts) then did the final sum in its head. Adding ordering guidance to the system prompt
    reliably fixed this → `convert×3 → add`.
  - *Argument typing:* the model often emits numbers/arrays as JSON strings (`"100"`,
    `"[1,2,3]"`), which the MCP server's JSON-schema validation rejects (`string found, number
    expected`). It then HALLUCINATES results and gives a confident wrong answer. Adding an
    explicit "pass JSON numbers, not strings" instruction to the system prompt fixed the typing.
  - *Data threading (unsolved by prompting):* even with correct order + types and all 4 tool calls
    succeeding, the model does NOT feed the real convert outputs (79.0/42.93/25.16) into `add` —
    it fabricates the addends every run (e.g. [23.32,21.91,184.19] → 229.42; correct is 147.09).
    This is a model-capability ceiling for an 8B model, not a code bug.
  - **Lesson:** the loop infra is correct and fully observable; final accuracy is bottlenecked by
    the model. Natural next experiment: swap `AGENT_MODEL` to a stronger tool-capable model and
    re-run — expect the data-threading to improve. (This is the plan's "model drives reliability"
    point, demonstrated.)
