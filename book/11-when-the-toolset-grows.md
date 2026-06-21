# Chapter 11 — When the Toolset Grows

*(Phase 6 · the part nobody warns you about)*

> **What we wanted to learn → What we built → What actually happened → What it taught us.**

Chapter 10 added a third specialist server — fees/tax — so the *planner* would have something worth
decomposing. Harmless, we thought: the planner reads the whole tool catalog, the chat ignores it.
Then the chat — which had been rock-solid since Chapter 4 — started getting currency totals *wrong*.
This chapter is the debugging story, because the fix is less interesting than what the failures
taught us about small models and tools.

## What actually happened (the regression)

The agent wraps **every** connected MCP tool. Adding fees/tax quietly grew the chat's toolset from
5 tools to 7. With that, `qwen2.5:14b` started failing the basic *"100 USD + 50 EUR + 5000 JPY in
GBP"* — skipping the USD conversion, calling `add` on the raw amounts, sometimes inventing numbers.
The unsettling part: it failed **even though it never called the two new tools.** Their mere
*presence* in the prompt was enough to derail it.

That's the first lesson, and it's counterintuitive: **more tools degrade a small model even when
the extra tools are irrelevant and unused.**

## The experiments (what we tried, in order)

We resisted the easy fix and tried to fix it *in the model's context* first:

1. **Tell it to ignore irrelevant tools.** Adding "use only the tools the task needs; don't call
   fee/tax unless asked" stopped it from *calling* the new tools — but it still mis-threaded the
   conversions. Steering tool *selection* didn't fix tool *use*.
2. **A worked example.** A few-shot example ("convert each amount, then add the converted values")
   pushed it back to ~75–80% correct. Better — but now the system prompt enumerated tool names and a
   currency walkthrough. It worked by *coupling the prompt to the toolset*, which felt like fine-tuning
   by prompt: brittle, and it would rot the moment the tools changed.
3. **A bigger model.** This is the project's recurring move (Chapter 4), so we tried it: pulled
   `qwen2.5:32b` (19 GB — nearly the whole machine) and pointed `AGENT_MODEL` at it. The result was
   the most instructive failure of the whole project:

   ```
   convert 100 USD → GBP   = 79.0      ✓
   convert  50 EUR → GBP   = 42.93     ✓
   convert 5000 JPY → GBP  = 25.16     ✓
   add [83.33, 41.67, 32.91] = 157.91  ✗   ← fabricated; ignored its own tool results
   ```

   It converted **perfectly**, then fed `add` three **made-up** numbers and reported 157.91 instead
   of 147.09 — *consistently*, every run, even though the prompt explicitly said "pass the actual
   converted values returned by convert." A bigger model, slower (~30 s/run vs single digits) and
   four times the RAM, and it **still** wouldn't thread its own tool outputs.

   That killed a comfortable assumption: **bigger ≠ automatically more reliable.** Model size buys
   raw capability, not necessarily faithfulness to tool results.

## What we shipped, and why

We stopped fighting the model and fixed it where the problem actually was — *scope and order*:

- **Scope the chat by an include-list (whitelist).** The chat is a currency+calculator assistant;
  it declares the servers it uses (`currency-tools`, `calculator-tools`). Anything else — today's
  fees/tax, tomorrow's whatever — stays out by default. The *planner* still discovers every server.
  This isn't hiding tools; it's matching the toolset to the use case. (A whitelist beats a blacklist
  here: new servers don't silently leak into the chat.)
- **Order the tools sensibly.** Connecting a third server had shuffled the tool list so `add`
  appeared *before* `convert` — and the model, meeting aggregation first, jumped to it. Presenting
  conversions before aggregation flipped reliability from ~2/4 to **6/6**. Tool *order* is a real,
  generic lever for a small model, and it costs nothing.

With those two changes the system prompt went back to **plain** — no tool catalog, no worked
example. Clean *and* reliable, on the fast 14b. That's why we chose this over the prompt hacks: it
removes the coupling instead of adding more of it.

The honest caveat: scoping + ordering *mitigates*; it doesn't make a small local model infallible.
Multi-step tool threading is simply near the edge of what a 14B model does reliably. Which raises the
obvious question.

## What about Gemini, or Claude?

Everything above is a *small-local-model* problem. A frontier hosted model — Claude or Gemini —
would take the full seven-tool set, no whitelist, no ordering tricks, no worked example, and thread
the conversions correctly, because the bottleneck here is model capability, not architecture.

And switching is genuinely small, because we built for it back in Chapter 7:

- The agent talks to the model through Spring AI's provider-neutral `ChatClient`, and reports cost
  through a provider-neutral `Usage`. Moving to a hosted model is: **swap the starter** (e.g.
  `spring-ai-starter-model-anthropic` or `-vertex-ai-gemini`), **set the config** (model id + API
  key), and **add one row to `Pricing`**. No agent-loop, capture-hook, or endpoint code changes.
- You can also reach Claude through **Amazon Bedrock** or **Google Vertex AI** if that's where your
  keys live — same Messages-style API, same neutral abstractions.

The trade-offs are the real lesson, not the vendor:

| | Local (qwen2.5:14b) | Hosted frontier (Claude / Gemini) |
|---|---|---|
| Reliability on 7-tool threading | mitigate (scope + order) | handles it directly |
| Cost | $0 | per-token (watch `usage`) |
| Privacy | stays on your machine | data leaves the box |
| Latency | local, model-load heavy | network, but fast inference |
| Ops | `ollama pull` | API key + rate limits |

So the project's north star — *the model is the reliability knob* — holds at the largest scale too:
when a task sits at the edge of a small model, you can buy your way past it with a better model, and
the bill (and the privacy cost) is the thing you weigh. We kept the local model for this learning
build precisely so the limits stay visible — but the door to a frontier model is one starter and one
config block away.

## What it taught us

- **Adding a tool is not free.** Every tool you expose is context the model must reason around —
  more tools can lower reliability even when they're never called. Give each agent only the tools its
  job needs.
- **Bigger isn't a guaranteed fix.** A 32B model converted flawlessly and still fabricated the sum.
  Capability and faithfulness-to-tools are different axes.
- **Scope and order are real levers** — cheaper and more honest than prompt scaffolding that pins the
  prompt to a specific toolset.
- **Know where your ceiling is.** A small local model is probabilistic at multi-step tool use; you
  mitigate it, or you change the knob that actually moves it — the model.

## The code

- [`chat/AgentService.java`](../agent/src/main/java/com/example/agent/chat/AgentService.java) — the chat toolset whitelist + tool ordering
- [`application.yml`](../agent/src/main/resources/application.yml) — `agent.chat.tool-servers` (the include-list)
- See also [ADR-0007](../docs/adr/0007-scope-agent-toolset.md).

## Try it yourself

See the regression and the fix for yourself by toggling the whitelist (no rebuild — it's config):

```bash
# scoped (default): reliable
curl -s localhost:8080/agent/run -H 'Content-Type: application/json' \
  -d '{"input":"Add 100 USD, 50 EUR and 5000 JPY and give the total in GBP."}' | jq '.steps[].tool, .answer'

# widen the chat to ALL servers and watch reliability drop:
#   set agent.chat.tool-servers to blank (or add feestax-tools) in application.yml, restart, re-run

# or change the knob that matters — a bigger/hosted model:
#   AGENT_MODEL=qwen2.5:32b mvn -pl agent spring-boot:run     # slower, heavier; see above
```

Run the scoped version a few times — convert×3 then a single add of the converted values, every
time. Then widen the toolset and watch the small model wobble. That wobble, and what does and
doesn't fix it, is the whole chapter.

---

→ [Epilogue — What the Loop Taught Us](12-epilogue.md)
