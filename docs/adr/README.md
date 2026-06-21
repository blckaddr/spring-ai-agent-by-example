# Architecture Decision Records

One file per decision: `NNNN-short-title.md` (zero-padded, sequential). Use
[`0000-template.md`](0000-template.md) as the starting point.

## Conventions

- **Immutable once accepted.** Don't rewrite history — if a decision changes, add a *new* ADR
  that supersedes the old one and update the old one's status to `Superseded by ADR-NNNN`.
- **Status** is one of: `Proposed` · `Accepted` · `Superseded` · `Deprecated`.
- Keep each ADR short: the context, the decision, and the consequences (good and bad).

## Index

| ADR | Title | Status |
|-----|-------|--------|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](0002-local-llm-via-ollama.md) | Local LLM via Ollama with a tool-capable model | Accepted |
| [0003](0003-tools-over-mcp-only.md) | Agent reaches tools only over MCP/HTTP | Accepted |
| [0004](0004-phased-learning-build.md) | Build in gated learning phases | Accepted |
| [0005](0005-spring-ai-2-on-boot-4.md) | Build on Spring AI 2.0 + Spring Boot 4 | Accepted |
| [0006](0006-phase-6-plan-only.md) | Phase 6 is "plan-only" (decompose & visualize), not multi-agent execution | Accepted |
| [0007](0007-scope-agent-toolset.md) | Scope each agent's toolset to its use case (whitelist) | Accepted |
| [0008](0008-agent-packages-by-concern.md) | Organize the agent module by concern, one-way deps | Accepted |
