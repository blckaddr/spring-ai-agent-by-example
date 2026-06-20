# Phase 0 — Sync loop, one MCP server, step visibility

**Kind:** reasoning (foundation)

> See [README.md](README.md) for operating rules, architecture, and the full phase ladder.

## Learning goal

See the tool-calling loop happen at all. Make the agent's reasoning *visible* instead of a
black box.

## Build

- Parent POM + two modules: `mcp-server-currency`, `agent`. `git init`.
- `mcp-server-currency`: streamable-HTTP MCP server exposing `convert(amount, from, to)`
  and `listRates()`. (Static `@Tool` methods are fine — the human's real MCP is dynamic,
  but that risk is already proven elsewhere; this demo doesn't need to re-prove it.)
- `agent`: Ollama ChatClient + MCP client pointed at the currency server. One **synchronous**
  endpoint, e.g. `POST /agent/run` taking a natural-language request, returning
  `{ "answer": ..., "steps": [ ... ] }`.
- **Capture hook:** intercept each tool call in the loop (advisor / tool-call metadata) and
  record `{ step, tool, server, arguments, result, latencyMs }`. Log each step to the console
  AND collect them into the `steps[]` returned in the response. **This hook is reused in every
  later phase — build it cleanly.**

## Do NOT build yet

Second MCP, memory, async, streaming, UI.

## Done when

A request like "convert 100 USD to EUR" returns the right answer AND the `steps[]` (and
console log) clearly show the `convert` tool being called with the right args and result.
The human can *see the loop*.

## Next

[Phase 1](phase-1-second-mcp.md) adds a second MCP server and a task that genuinely requires
a multi-step, dependent loop.
