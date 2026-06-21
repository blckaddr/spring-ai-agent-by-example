# NOTES — running state & decisions

Scratchpad for facts that change as the build proceeds. Keep it current; it's the memory
between sessions. (Contract & rules live in [`CLAUDE.md`](CLAUDE.md); the plan in [`plans/`](plans/).)

## Environment (fill in once verified)

| Thing | Value | Verified? |
|-------|-------|-----------|
| Ollama installed | yes — client `0.21.2` | ☑ (2026-06-20) |
| Ollama endpoint | `http://localhost:11434` | ☑ |
| Ollama server running? | **NO at check time** — start with `ollama serve` or the desktop app before running the agent | ☐ |
| Model tag (RECOMMENDED, reliable) | **`qwen2.5:14b`** (~9GB, ~18GB resident) — threads tool outputs correctly; gets the multi-step task right 3/3 | ☑ (2026-06-20) |
| Model tag (default config) | `llama3.1:8b` — gets loop SHAPE right but fabricates `add` inputs; good for the "model matters" lesson | ☑ |
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
| 2 — failure & recovery | ☑ done & verified | (see git log) | cleaner error capture in RecordingToolCallback; probed unknown-currency failures with qwen2.5:14b — recovers (apologize/list, or substitute+retry), no hallucination; mid-chain failure halts cleanly. Book Ch5. |
| 3 — memory / multi-turn | ☑ done & verified | (see git log) | MessageChatMemoryAdvisor + MessageWindowChatMemory (in-memory), keyed by sessionId. Turn 2 "that total" resolved from turn 1 via memory; control session had no context (0 steps). Book Ch6. |
| 3.5 — cost/usage observable | ☑ done & verified | (see git log) | Response-level usage in AgentResponse.usage: tokens (final round) + wallClockMs (full run) + $ estimate (Pricing table, $0 local). ChatModel-wrapper approach abandoned (loop runs inside the model). Book Ch7. |
| 4 — async (202 + runId + poll) | ☑ done & verified | (see git log) | POST /agent/runs -> 202+runId (detached on ThreadPool executor); in-memory RunStore (QUEUED/RUNNING/DONE/FAILED) persists answer+steps+usage; GET /agent/runs/{id} polls. Sync /agent/run kept. eventId idempotency ✓. Safety caps: max-steps (hard FAILED) ✓, max-wall-clock timeout. Book Ch8. |
| 5 — streaming live (SSE) | ☑ done & verified | (see git log) | GET /agent/stream (SseEmitter) pushes each Step live via a step listener on the capture hook; the chat page (EventSource) renders it (static/index.html at Phase 5; later moved to static/chat.html at /chat). Verified via timestamped curl: events smeared across ~45s as the loop runs (not batched). Book Ch9. |
| 6 — "Watch it Plan" (optional) | ☑ done & verified | (see git log) | Plan-ONLY phase (no execution). New 3rd MCP server `mcp-server-feestax` (:8083, transactionFee/taxRate). `ToolCatalog` discovers all servers' tools over MCP; `PlanService` = one tool-less model call → JSON plan graph `{nodes:[{id,specialist,op,summary,inputs}]}`, validated (unique ids, no dangling refs, acyclic/Kahn). `POST /agent/plan` → {graph,catalog,usage}; `/plan` page renders it with vendored Mermaid (subgraph per specialist). Single-agent path untouched. Earlier "team execution" draft reverted (incl. Step.lane). Book Ch10. |

## Decisions log

- _(date)_ Plan split into `plans/` overview + per-phase files; added `CLAUDE.md` + `NOTES.md`.
- _(2026-06-21)_ **Phase 6 re-scoped** from "planning / multi-agent" to **"Watch it Plan"** —
  plan-only, no execution: the model decomposes a goal into a validated dependency graph, rendered in
  the browser. The earlier orchestrator + sub-agents *execution* draft was dropped (the per-task
  sub-agent LLMs were thin one-tool wrappers — pure overhead). Plan file renamed
  `phase-6-multi-agent.md` → `phase-6-watch-it-plan.md`.
- _(2026-06-21)_ **Memory is opt-in.** `AgentService.run` now mints a fresh `anon-<uuid>` conversation
  id when no `sessionId` is supplied (was a shared, accumulating `"default"` session). So single-shot
  chat calls (the book's `curl` examples) are independent and reproducible no matter how often / in
  what order they run; memory persists only with an explicit `sessionId` (Phase 3 / Ch 6). The UI
  already sends its own id, so it's unaffected.

## Open questions / things to confirm with the human

- _(none open — model default settled: config default `llama3.1:8b`, recommended `qwen2.5:14b`;
  see the env table above. Add new ones as they come up.)_

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

- **Phase 2 (qwen2.5:14b), failure & recovery.**
  - *Predictive refusal:* `convert(…, "ZZZ")` → the model made ZERO tool calls (knew ZZZ isn't a
    real ISO code) and asked for a valid one. To trigger a real tool failure, use a valid ISO code
    absent from the demo table (e.g. AUD; table = USD/EUR/GBP/JPY/CHF/CAD).
  - *Recovery:* on `convert(USD→AUD)` failure, the clean error (incl. "Known codes: [...]") is fed
    back; the model recovers — either apologize+list supported, or substitute a valid currency
    (CAD) and retry. Never hallucinates a rate (unlike llama3.1:8b in Phase 1).
  - *Mid-chain failure:* "Add 100 USD + 50 AUD → GBP" converts USD ok, AUD fails → model HALTS
    (no `add`, no fabricated value), reports partial + reason. Correct.
  - *Quirk:* one mid-chain run answered correctly but IN THAI (qwen language drift). Reasoning fine,
    language unexpected — reading steps[] beats trusting the answer prose.
  - *Engineering lesson:* error-message QUALITY drives recovery (model used our "Known codes"
    list). Code change this phase: `RecordingToolCallback.cleanError()` extracts the inner `text=`
    from the wrapped MCP error.

- **Phase 3.5 (cost/usage).** AgentResponse.usage = {promptTokens, completionTokens, totalTokens,
  wallClockMs, estimatedCostUsd, model, note}. Source: final ChatResponse.getMetadata().getUsage().
  - *Scope honesty:* tokens are the FINAL round's (Spring AI runs the tool loop INSIDE the chat
    model, so per-round token sums aren't cleanly accessible without the tracing stack we skip).
    wallClockMs IS the full-run time and grows with loop length (e.g. 9.6s/1-call vs 27.6s/7-call).
  - *Dead end recorded:* wrapping the ChatModel bean (RecordingChatModel + BeanPostProcessor) broke
    model-name seeding (`model cannot be null`) AND is the wrong layer (loop is inside the model) →
    removed. Use response-level capture.
  - *Provider portability:* Usage is provider-neutral → switching to Gemini/OpenAI = swap starter +
    config + add one row to Pricing. Cost grows with loop rounds (context resent) + memory.

- **Phase 6 (qwen2.5:14b), "Watch it Plan" — plan-only, no execution.** Re-scoped from the earlier
  team-execution draft (dropped: the sub-agent LLMs were thin one-tool wrappers — pure overhead).
  - *What it does:* `POST /agent/plan` makes ONE tool-less model call; the planner reads the live
    tool catalog (discovered over MCP from all 3 servers) and returns a JSON plan graph. No tool is
    ever executed — the graph IS the deliverable, rendered at `/plan` with Mermaid.
  - *Rich domain works:* added `mcp-server-feestax` (:8083, transactionFee/taxRate, demo values).
    The consolidation goal produced a **19-node DAG across 4 specialists** (currency 5 / feestax 8 /
    calculator 5 / orchestrator 1): listRates→converts→(fee,tax)→multiply→add→select. Valid
    (acyclic, all refs resolve). 1,339 tokens, 1 call, ~59s.
  - *Plan quality — initially imperfect, then fixed via the planner prompt:* the FIRST planner
    collapsed comparison goals to USD-only (no real comparison), misfiled `convert` under
    calculator-tools, and used an odd `multiply(value, taxRate, fee)`. The graph made the flaw
    obvious (a "cheapest" select with ONE input). Fix = planner-prompt guidance: enumerate candidates
    + one branch per candidate for cheapest/best goals; attribute each op to its real server; keep
    units consistent. After: EX1 simple ✓, EX2 portfolio ✓, EX3 cheapest-of-3 → branches per
    candidate + select over all 3, **4/4 valid, 0 misfiled ops**. Residual: 5-holding consolidation
    sometimes drops a node ref (too big for 14b) → validator 422 → re-run. Non-deterministic.
  - *Validation earns its keep:* PlanService rejects non-JSON, duplicate ids, dangling `inputs`, and
    cycles (Kahn's), throwing PlanException with the raw text → 422 shown on the page.
  - *REGRESSION (toolset bleed) — full saga, see book Ch 11.* Adding the feestax MCP connection grew
    the **chat's** toolset 5→7 (the agent wraps ALL connected MCP tools). qwen2.5:14b then failed the
    basic GBP-sum (skipped USD, called `add` on raw amounts, even fabricated inputs). Notable: the
    mere PRESENCE of the 2 extra tools degraded threading even when they were never called.
    - *Experiments (all measured):* (a) prompt domain-steering → stopped fee/tax leakage but NOT the
      mis-threading; (b) few-shot worked example → ~75-80% but couples the prompt to the toolset
      (felt like prompt fine-tuning — rejected); (c) **bigger LOCAL model** `qwen2.5:32b` (19 GB,
      ~30s/run) → converted perfectly then fed `add` FABRICATED numbers `[83.33,41.67,32.91]`=157.91,
      **consistently** (4/4), ignoring its own tool results despite the explicit "pass returned
      values" rule. Bigger ≠ reliable; capability ≠ faithfulness to tool outputs.
    - *Shipped fix (Option A):* (1) **whitelist** the chat's servers — `agent.chat.tool-servers:
      currency-tools,calculator-tools` (include-list, not blacklist, so future servers stay out by
      default; planner still sees all via ToolCatalog); (2) **order** the tools currency-before-
      calculator so the model meets convert before add — this alone flipped ~2/4 → **6/6**. System
      prompt back to PLAIN (no catalog, no worked example). Fast 14b. feestax → planner only.
    - *Lessons:* more tools degrade a small model even when unused; tool **selection + order** are
      real reliability levers; a small local model is probabilistic at multi-step tool threading —
      mitigate (scope+order) or change the knob that matters (a frontier hosted model: Claude/Gemini
      handle the full 7-tool set directly; switching = swap starter + config + a Pricing row, thanks
      to the provider-neutral ChatClient/Usage from Ch 7).
    - *Model note:* `qwen2.5:32b` pulled (19 GB) for the experiment; NOT the default (slower, heavier,
      no reliability gain). `ollama rm qwen2.5:32b` to reclaim space. Added to Pricing as local/$0.
  - *Mermaid gotcha:* `mermaid@11 dist/mermaid.min.js` is a **UMD/global** build (sets
    `globalThis.mermaid`), NOT an ESM module — `import mermaid from …` yields undefined. Load it as a
    classic `<script src>` and use the global. (The ESM build is `mermaid.esm.min.mjs` + chunks,
    not a single vendorable file.)
  - *Version gotcha (still true):* Boot 4.1 ships **Jackson 3** — `tools.jackson.databind.ObjectMapper`
    (not `com.fasterxml.jackson.databind`), exceptions unchecked (`tools.jackson.core.JacksonException`).
    Annotations stay on `com.fasterxml.jackson` 2.x.

- **Model-swap experiment (qwen2.5:14b) — RESOLVES layer 3.** Same code, same prompts, only
  `AGENT_MODEL=qwen2.5:14b`. The model threads its own convert outputs into `add`:
  `add([79, 42.93, 25.16]) = 147.09` — CORRECT, 3/3 runs. Confirms the data-threading failure was
  a model-capability ceiling, not a code/prompt bug. Cost: slower (seconds vs ms) + ~18GB resident
  vs ~6GB. Documented as book Chapter 4.
