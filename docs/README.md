# docs

Project documentation. Two kinds live here:

- [`adr/`](adr/) — **Architecture Decision Records.** One file per decision, numbered and
  immutable once accepted. Captures *why* a choice was made, not just *what*.
- [`architecture.md`](architecture.md) — **Architecture doc.** Living description of how the
  system is shaped (components, boundaries, the agent loop, data flow). Updated as the build
  grows phase by phase.
- [`troubleshooting.md`](troubleshooting.md) — **Troubleshooting.** Common issues running it
  (no tools firing, Ollama, ports, language drift, push errors).

## How docs relate to the rest of the repo

| Source | Holds |
|--------|-------|
| [`../CLAUDE.md`](../CLAUDE.md) | The working contract / hard rules for building. |
| [`../plans/`](../plans/) | The phased build plan (what to build, in order). |
| [`../NOTES.md`](../NOTES.md) | Mutable running state — env, versions, phase status. |
| `docs/adr/` | Decisions and their rationale (durable, append-only). |
| `docs/architecture.md` | How the system is currently shaped. |

Rule of thumb: if it's a **choice with trade-offs** → ADR. If it's **how things fit
together** → architecture. If it **changes constantly** → NOTES.
