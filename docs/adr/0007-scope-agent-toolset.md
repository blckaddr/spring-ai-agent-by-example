# ADR-0007 — Scope each agent's toolset to its use case (whitelist), not all connected tools

- **Status:** Accepted
- **Date:** 2026-06-21

## Context

The agent wraps the tools it gets from its MCP clients. Through Phase 5 it connected to two servers
(currency, calculator) = 5 tools, and `qwen2.5:14b` reliably handled the multi-step currency task.

Phase 6 added a third server (fees/tax) so the *planner* would have a richer domain. Because the
chat agent wrapped **all** connected tools, its toolset silently grew 5 → 7 — and the small model
started failing the basic currency total: skipping a conversion, calling `add` on raw amounts, even
fabricating inputs. Crucially, it degraded **even though it never called the two new tools** — their
mere presence in the prompt was enough.

We tried to fix this in the model's context first, and measured each attempt (see book Ch 11):

- **Prompt domain-steering** ("use only the tools you need") — stopped the fee/tax leakage but did
  not restore conversion threading.
- **A worked example** (few-shot) — got it to ~75–80%, but coupled the system prompt to the specific
  toolset (prompt fine-tuning that rots when tools change).
- **A bigger local model** (`qwen2.5:32b`, 19 GB) — slower (~30 s/run) and *still* fabricated the
  sum, ignoring its own correct tool results, consistently.

The only thing that was reliably correct was giving the chat just the tools it needs.

## Decision

Scope each agent's toolset to its use case. The **chat** is whitelisted to its required servers
(`agent.chat.tool-servers: currency-tools,calculator-tools`) and presents tools in a sensible order
(conversions before aggregation). The **planner** still discovers *every* server via `ToolCatalog`
(it needs the full catalog to decompose). Scoping is config-driven (server names, resolved through
`McpToolServerIndex`) — an *include* list, so any future server stays out of the chat by default.

## Consequences

- **Good:** reliable chat with a **plain, generic** system prompt (no toolset-specific scaffolding);
  fast (14b); the planner is unaffected. A whitelist (vs blacklist) is future-proof.
- **Bad / cost:** the chat can't answer fee/tax questions (that's the planner's domain); the chat
  and planner now expose different toolsets — a small asymmetry to keep in mind.
- **Follow-ups:** the real ceiling is **model capability**, not architecture — a frontier hosted
  model (Claude/Gemini) handles the full toolset directly. The provider-neutral `ChatClient`/`Usage`
  (ADR-0002, Phase 3.5) make that swap a starter + config + one `Pricing` row. See book Ch 11.

## Alternatives considered

- **Expose all tools + prompt scaffolding** — works ~80%, but couples the prompt to the toolset and
  is still flaky. Rejected: brittle and dishonest about the limit.
- **Expose all tools + a bigger local model** — `qwen2.5:32b` was slower, heavier, and still
  unreliable at tool-result threading. Rejected: no reliability gain.
- **Tool selection purely at call time** — Spring AI passes the toolset per call already; the issue
  is *which* tools are in scope, which is what this ADR settles.
