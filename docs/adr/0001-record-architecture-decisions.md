# ADR-0001 — Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-06-20

## Context

This is a learning project built in gated phases, likely across multiple sessions. Decisions
made early (model choice, transport, boundaries) need to survive context loss between sessions
and explain *why* later code looks the way it does.

## Decision

Keep lightweight ADRs in `docs/adr/`, one file per decision, numbered sequentially, immutable
once accepted. Use the [template](0000-template.md). Index them in the ADR
[README](README.md).

## Consequences

- **Good:** rationale is durable and discoverable; a fresh session can reconstruct *why*
  without re-deriving it. Reinforces the learning goal (decisions are explicit, not implicit).
- **Bad / cost:** small upkeep cost per decision.
- **Follow-ups:** add an ADR whenever a choice has real trade-offs; mutable state stays in
  `NOTES.md`, not here.

## Alternatives considered

- **No ADRs, rely on commit messages** — too thin; rationale gets lost and isn't browsable.
- **One big decisions doc** — harder to keep immutable and to reference a single decision.
