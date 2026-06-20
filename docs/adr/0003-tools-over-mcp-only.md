# ADR-0003 — Agent reaches tools only over MCP/HTTP

- **Status:** Accepted
- **Date:** 2026-06-20

## Context

Tools (currency convert, calculator add) could be invoked by the agent in several ways: direct
Java method calls, a shared library, or a remote protocol. The learning goal is to understand
real agentic systems, where tools are independently-deployed services the agent discovers and
calls over a protocol — not local function calls.

## Decision

Tools live in **separate Spring Boot MCP servers** (streamable-HTTP). The agent reaches them
**only over MCP/HTTP** via an MCP client — never by importing a server's Java classes. The
agent loop never lives inside an MCP server; servers stay passive tool providers. Agent state
(when introduced) lives **outside the request**, not in controller/instance fields.

## Consequences

- **Good:** clean agent/tool boundary; mirrors real deployments; multi-server tool aggregation
  becomes natural (Phase 1 adds a second server); name-collision handling becomes a real,
  teachable concern.
- **Bad / cost:** more moving parts to run (multiple Boot apps); network/transport config to
  get right; harder to debug than in-process calls.
- **Follow-ups:** verify current MCP client/server streamable-HTTP artifact IDs + config keys
  against live Spring AI docs before wiring (see `NOTES.md`).

## Alternatives considered

- **In-process `@Tool` beans in the agent** — simpler, but erases the boundary that's the
  whole point; you'd learn function calling, not agentic systems.
- **Shared tools library imported by the agent** — couples agent to tool internals; not how
  MCP works.
