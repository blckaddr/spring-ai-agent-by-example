# ADR-0005 — Build on Spring AI 2.0 + Spring Boot 4

- **Status:** Accepted
- **Date:** 2026-06-20

## Context

Spring AI's version lines are tied to Spring Boot major versions:

| Spring AI line | Latest GA | Spring Boot |
|---|---|---|
| 1.1.x | 1.1.8 | 3.5.x |
| 2.0.x | 2.0.0 | 4.x |

So "use Spring AI 2.0" is really "use Spring Boot 4 + Spring AI 2.0" — a two-framework
choice, not a point upgrade. Both options support Java 21 and the streamable-HTTP MCP
transport this project requires. The trade-off is **maturity / abundance of learning material**
(1.1.x + Boot 3, the larger ecosystem in mid-2026) vs **current-generation stack**
(2.0 + Boot 4, future-proof, matches the default docs).

A real trap regardless of choice: Maven Central's "latest" Spring AI now resolves to 2.0.0,
which silently pulls Spring Boot 4 — so the version must be pinned explicitly either way.

## Decision

Build on **Spring AI 2.0.0 + Spring Boot 4.1.0** (Spring AI 2.0.0's starters resolve to
Spring Boot 4.1.0). Java 21. The human chose the current-generation stack deliberately, to
learn today's APIs rather than the prior line.

Pinned coordinates (verified present on Maven Central 2026-06-20):
- BOM: `org.springframework.ai:spring-ai-bom:2.0.0`
- Parent: `org.springframework.boot:spring-boot-starter-parent:4.1.0`
- Starters (BOM-managed, no explicit version): `spring-ai-starter-model-ollama`,
  `spring-ai-starter-mcp-client`, `spring-ai-starter-mcp-server-webmvc`

## Consequences

- **Good:** current APIs; matches the default (unversioned) docs tree; future-proof.
- **Bad / cost:** both frameworks are recently GA — fewer worked examples and SO/blog answers
  when debugging; more chance of being first to hit an issue. 2.0 changed some property names
  (`spring.ai.ollama.chat.temperature` — no `.options.` prefix; model enablement via
  `spring.ai.model.chat`) and moved MCP annotation packages to `org.springframework.ai.mcp.annotation`.
- **Follow-ups:** always reference the **unversioned** docs (= 2.0), never the `/reference/1.1/`
  tree. Verified config keys are recorded in [`NOTES.md`](../../NOTES.md). If 2.0/Boot 4
  friction blocks *learning*, the fallback is the 1.1.8 + Boot 3.5.15 line (this ADR would then
  be superseded).

## Alternatives considered

- **Spring AI 1.1.8 + Spring Boot 3.5.15** — more mature, the largest pool of matching
  tutorials, and matches the plan's original Boot-3.x assumption. Rejected in favor of learning
  the current stack; kept as the documented fallback.
