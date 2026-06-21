# Epilogue — What the Loop Taught Us

We started with an agent that could barely be seen: one tool call, vanishing inside a single
blocking model call (Chapter 2). We ended watching a model **decompose a goal into a plan** and
render it as a graph (Chapter 10), with a debugging story about growing toolsets along the way
(Chapter 11). Between those two points is the whole lesson, and it's smaller than the page count
suggests.

## The one decision that paid for everything

Phase 0's **step-capture handrail** — recording each tool call as `{ step, tool, server, arguments,
result, latencyMs }` — was the through-line. We built it once and never rebuilt it: it was *logged*
(Ch 2), *chained* across a multi-step task (Ch 3), *error-annotated* (Ch 5), *memory-aware* (Ch 6),
*priced* (Ch 7), *persisted* for detached runs (Ch 8), and finally *streamed live* (Ch 9). "Watch it
think" cost almost nothing because the steps were captured on day one. If there's a single
takeaway for your own agents: **decide how you'll see the loop before you build the loop.**

## The lessons that kept recurring

- **The model is the reliability knob.** The same code went from fabricating sums (8B) to threading
  them correctly (14B) with no change but `AGENT_MODEL` (Ch 4) — and a 32B model *still* fabricated a
  sum it had just computed (Ch 11). Capability, not architecture, sets the ceiling.
- **Prompting fixes format, not capability.** Ordering, JSON types, "convert each amount once,"
  "reuse that total" — all stuck (Ch 9). "Answer in English" never did. Knowing which failures a
  better prompt can fix and which need a better model is most of the skill.
- **Trust the steps, not the prose.** A confident paragraph hid a wrong answer; the `steps[]` (Ch 5)
  and later the *plan graph* (Ch 10) made the truth — and the flaws — impossible to miss.
- **Observability is a design stance, not a tool.** No Micrometer, no OpenTelemetry. A
  `Consumer<Step>` and an `EventSource` were enough, because the system exposed its own steps from
  the start.
- **Every tool you add is context the model must reason around.** More tools lowered reliability
  *even when they were never called* (Ch 11). Give each agent only what its job needs.
- **The loop is the cost multiplier, and memory carries mistakes forward.** Cost grows with rounds
  and remembered context (Ch 7); memory faithfully replays the bad along with the good (Ch 6).

## Where the frontier goes next

We stopped at the honest edge. Beyond it is real agent-systems territory, noted but not built:

- **Executing** the validated plan graph — model as planner, deterministic code as runtime.
- **Re-planning** loops and **evaluator** agents that judge and retry.
- **Tool/agent registries**, and reaching for a **frontier hosted model** (Claude, Gemini) when a
  task sits past a small model's ceiling — a swap the provider-neutral `ChatClient`/`Usage` already
  make cheap (Ch 7, Ch 11).

Each of those is another rung. None of them change the habit that carried us up this one: build the
agent so you can *see it work*, then let what you see tell you what to fix next.

## If you followed from Chapter 1

You didn't just wire up an agent. You watched, at each rung, *where the hard parts actually are* —
the model's reliability ceiling, the value of a clear error message, that memory carries mistakes
forward, that the loop is the cost multiplier, that a bigger toolset can make a small model worse.
And you have a glass-box agent you can see straight through. That was the whole goal.

---

*The end — now go build one you can see straight through.* 🔍
