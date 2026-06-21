# ADR-0008 — Organize the agent module by concern, with one-way package dependencies

- **Status:** Accepted
- **Date:** 2026-06-21

## Context

By the end of Phase 6 the `agent` module had ~27 classes flat in a single package
`com.example.agent`. For a build whose whole point is *one concept per phase*, a flat package hides
the structure the project is trying to teach — the reusable core (the step-capture handrail) sits
indistinguishable among feature classes, and there's no visible boundary between phases.

## Decision

Group the classes into by-concern sub-packages, with `AgentApplication` left at the root so Spring
Boot component-scan still covers everything below it:

- `capture/` — the step-capture handrail (Step, StepCollector, StepCapture, RecordingToolCallback,
  LoopLimitExceededException)
- `mcp/` — MCP plumbing / tool discovery (McpToolServerIndex, ToolCatalog)
- `chat/` — the single reactive agent + its sync/stream surface
- `run/` — detached/async runs (Phase 4) + RunController
- `plan/` — the planning graph (Phase 6)
- `usage/` — cost/observability (Pricing, RunUsage)
- `config/` — Spring wiring (AsyncConfig, MemoryConfig, WebConfig)

To keep dependencies one-way, the async endpoints were extracted from `chat.AgentController` into a
new `run.RunController`, so `chat` no longer depends on `run` (dependency flows `run → chat`, not a
cycle). `capture`/`mcp`/`usage`/`config` are dependency-free leaves the features build on.

## Consequences

- **Good:** the package names mirror the phase ladder; the shared core (`capture`) is visibly the
  reusable through-line; one-way deps make the architecture legible; a reader can open one folder per
  concept.
- **Bad / cost:** a little visibility had to widen (`StepCapture` and its `start`/`clear` became
  `public` for cross-package use); one cross-feature import remains (`plan` reuses
  `chat.AgentRequest`) — a harmless one-way dependency we chose not to over-engineer away.
- **Follow-ups:** none. If the plan phase ever gains execution, `plan/` would naturally sprout an
  `exec/` sub-area.

## Alternatives considered

- **By layer** (`controller/`, `service/`, `model/`, `config/`) — the conventional Spring default,
  but it scatters each phase's concept across four folders, the opposite of what a "one concept at a
  time" book wants. Rejected.
- **Leave it flat** — simplest, but at ~27 classes it buries the structure and the handrail. Rejected
  now that the build is complete.
