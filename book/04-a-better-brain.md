# Chapter 4 — A Better Brain *(an experiment)*

## What we wanted to learn

Chapter 3 ended on a clean, testable hypothesis. The 8B model (`llama3.1:8b`) reliably got the
*shape* of the multi-step loop right — convert, convert, convert, add — but reliably failed to
carry its own tool results forward into the final `add`, fabricating the numbers instead. We
argued this was a **capability ceiling**, not a code or prompt problem.

There's a clean way to test that claim: **change nothing but the model.** Same agent code, same
system prompt, same tools, same task. If the failure is really about model capability, a stronger
model should fix it with zero code changes. If our code were subtly wrong, a better model
wouldn't save us.

This is exactly why, back in Chapter 1, the model name was a *configuration property* and never
hardcoded. We built the experiment in before we knew we'd run it.

## What we built

Nothing. That's the point.

We pulled a stronger tool-capable model — **`qwen2.5:14b`** (about 9 GB, ~14 billion parameters,
from the Qwen 2.5 family, which is well regarded for tool/function calling). Then we restarted
the agent with one environment variable:

```bash
AGENT_MODEL=qwen2.5:14b   # the only change. No recompile of logic, no prompt edit.
```

The agent's startup log confirmed it connected to both tool servers exactly as before. The only
thing different in the entire system was the brain.

## What actually happened

We ran the identical task — *"Add 100 USD, 50 EUR and 5000 JPY together and give me the total in
GBP"* — for which the correct answer is **147.09 GBP**.

```json
{
  "answer": "The total amount when adding 100 USD, 50 EUR and 5000 JPY together is approximately 147.09 GBP.",
  "steps": [
    { "step": 1, "tool": "convert", "arguments": "{\"amount\":100,\"from\":\"USD\",\"to\":\"GBP\"}", "result": "79.0"  },
    { "step": 2, "tool": "convert", "arguments": "{\"amount\":50,\"from\":\"EUR\",\"to\":\"GBP\"}",  "result": "42.93" },
    { "step": 3, "tool": "convert", "arguments": "{\"amount\":5000,\"from\":\"JPY\",\"to\":\"GBP\"}","result": "25.16" },
    { "step": 4, "tool": "add",     "arguments": "{\"numbers\":[79,42.93,25.16]}",                   "result": "147.09" }
  ]
}
```

Look at **step 4**. The three converts returned `79.0`, `42.93`, `25.16`. The model passed `add`
the numbers **`[79, 42.93, 25.16]`** — *its own actual tool results*, threaded forward exactly as
they should be. The total is **147.09**. Correct.

The exact thing the 8B model failed at every single time, the 14B model did effortlessly. And to
make sure it wasn't luck, we ran it three times:

| Model | `add` received | Result | Correct? |
|-------|----------------|-------:|:--------:|
| llama3.1:8b — run 1 | [23.32, 21.91, 184.19] | 229.42 | ✗ |
| llama3.1:8b — run 2 | [23.32, 21.91, 183.19] | 228.42 | ✗ |
| llama3.1:8b — run 3 | [23.99, 25.49, 5083.95] | 5133.43 | ✗ |
| **qwen2.5:14b — run 1** | **[79, 42.93, 25.16]** | **147.09** | **✓** |
| **qwen2.5:14b — run 2** | **[79, 42.93, 25.16]** | **147.09** | **✓** |
| **qwen2.5:14b — run 3** | **[79, 42.93, 25.16]** | **147.09** | **✓** |

The hypothesis held. Same code, same prompt, same tools — different brain, opposite outcome.

> A note on cost: the bigger model is slower (it takes seconds where the small one took
> milliseconds, and it occupies far more memory — roughly 18 GB resident vs ~6 GB). For a
> four-step task that's nothing. For a long loop it would add up. "Use a bigger model" is a real
> lever, but not a free one.

## What it taught us

- **The model is a swappable component, and its capability is a first-class design variable.**
  Not metaphorically — *literally* one environment variable changed correctness from "always
  wrong" to "always right." When an agent misbehaves, "is the model simply not good enough for
  this?" belongs at the top of your checklist, not the bottom.
- **Some failures are prompt-shaped; some are capability-shaped.** In Chapter 3, better prompts
  fixed ordering and argument-typing. They could *not* fix data-threading. Recognizing which kind
  of failure you have tells you whether to write a better prompt or reach for a better model —
  and stops you wasting hours prompt-engineering around a wall the model can't climb.
- **Designing for swappability paid off.** The single decision in Chapter 1 to make the model a
  config value turned a potentially invasive experiment into a one-line change. Cheap
  reversibility is what makes experiments like this worth running at all.
- **The capture hook, again, is what made the comparison possible.** "It works now" is a feeling.
  A table showing exactly what `add` received from each model is evidence. The whole reason we can
  *prove* the brain was the bottleneck is that we recorded every step from the very first phase.

We now have a setup where the loop is correct, observable, *and* backed by a model strong enough
to run it reliably. That's the right footing from which to do the thing we'd been deferring: stop
making the agent succeed, and start deliberately making its tools **fail** — to see how the loop
copes.

→ *Chapter 5 — When Tools Fail (Phase 2, upcoming)*
