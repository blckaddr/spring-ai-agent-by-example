# Chapter 7 — What Does It Cost? *(Phase 3.5)*

## What we wanted to learn

"Observable" should include *what it costs*, not just what it did. Every loop spends tokens and
time; on a hosted model those are dollars. So we set out to surface, per run: **tokens, wall-clock,
and an estimated cost** — and to do it in a way that survives swapping the model for a paid one.

A wrinkle unique to our setup: we run **locally via Ollama, so the dollar cost is literally $0.**
That doesn't make the exercise pointless — it makes the *proxies* the point. Tokens and time are
what *become* money on Gemini/OpenAI/Anthropic, so we surface those and layer an optional price
table on top.

## The approach that seemed right — and wasn't

The elegant idea: we'd already wrapped every *tool* in a recorder (Chapter 2). Why not wrap the
*model* the same way — a `RecordingChatModel` decorating the real one, capturing tokens and latency
on every call? Symmetry. We built it.

It broke immediately, and the failure taught us something about the framework:

1. **Wrapping the model bean disrupted Spring AI's plumbing.** The model name stopped reaching the
   call (`model cannot be null`) — the framework seeds the model into the request in a way that
   our wrapper interfered with.
2. **More fundamentally, it was the wrong layer.** In Spring AI, the tool-calling loop runs
   *inside* the chat model (the model re-invokes itself until done). So a wrapper around the model
   sees a *single* outer call, not one per round — it couldn't give us per-round data even if it
   worked.

So we deleted it. (That dead end is in the book on purpose: "wrap it symmetrically" was a
reasonable hypothesis, and learning *why* it's the wrong layer is worth more than hiding the
detour.)

## What we built instead

The robust path: read usage from the **final `ChatResponse`**. The agent already had the response
in hand; we just stopped throwing it away.

```java
ChatResponse response = chatClient.prompt()...call().chatResponse();
Usage usage = response.getMetadata().getUsage();   // promptTokens, completionTokens, totalTokens
String model = response.getMetadata().getModel();
```

Plus wall-clock timing around the whole call, and a small `Pricing` table turning tokens into a
dollar estimate (0 for local; illustrative hosted rates ready to use).

## What actually happened

```
Single convert (1 tool call):   prompt 661, completion 20, total 681,  wall-clock  9.6s,  $0
3-currency sum (7 tool calls):  prompt 926, completion 39, total 965,  wall-clock 27.6s,  $0
```

Read those two rows carefully — they contain the whole lesson:

- **Wall-clock is the honest total.** It nearly tripled (9.6s → 27.6s) as the loop grew from 1 call
  to 7. That's real, full-run cost, and it tracks loop length directly.
- **The token counts are the *final round's*, not the sum of all rounds.** If they were a true
  total, a 7-call run would dwarf a 1-call run — but 926 vs 661 is just "the last prompt was a bit
  bigger." Spring AI runs the loop inside the model, so the cleanly-accessible usage is the final
  (largest) exchange. It's a solid *proxy* for run cost, not a per-round ledger.
- **Cost is $0**, with `model: qwen2.5:14b` and a note saying tokens are the proxy.

We were honest about that scope in the code itself (`RunUsage`), because a cost number that
silently under-reports is worse than no number.

## How this changes on Gemini or another model

This is the payoff of building on Spring AI's abstractions — **the capture code doesn't change at
all:**

- `response.getMetadata().getUsage()` is provider-neutral; Gemini/OpenAI/Anthropic all populate it.
- Switching providers is: swap the starter dependency, change the config + credentials, point the
  model id. The agent logic, the usage capture, all untouched.
- Making the dollars real is **one row in the price table** (input and output rates — output is
  usually the pricier side).

And the cost *shape* to keep in mind once it's real money:

- **The loop is the multiplier.** Each tool round is a fresh model call that re-sends the growing
  context. A 7-round run isn't 7× a single message — it's worse, because every round re-pays for
  the accumulated context.
- **Memory (Chapter 6) adds to every turn**, since prior conversation is resent too.
- **A pricier-but-smarter model can be cheaper per *successful* task** — fewer wasted rounds, no
  hallucinated retries. Chapters 4 and 7 together let you actually measure that trade-off.

## What it taught us

- **Cost observability is easy to *approximate* and hard to make *exact*.** The useful 80% —
  tokens, wall-clock, a dollar estimate — is a few lines. The exact per-round token ledger lives
  behind the tracing/observability stack (Micrometer/OpenTelemetry) we've intentionally skipped.
  Knowing which side of that line you need keeps you from over- or under-building.
- **Match the hook to where the work happens.** We assumed model calls were the right seam; they
  weren't, because the loop lives inside the model. The same instinct that made tool-wrapping
  perfect made model-wrapping wrong.
- **An honest proxy beats a dishonest total.** Labeling the number "final round, not a sum" is what
  makes it trustworthy — the same principle as capturing tool errors instead of hiding them.
- **Abstractions earn their keep at the seams.** Because cost capture reads the provider-neutral
  `Usage`, "what would this cost on Gemini?" is a price-table row, not a rewrite.

The agent can now reason, orchestrate, recover, remember — and report what it spent. Everything so
far, though, has made the caller *wait* for the whole loop. Next we let it run detached.

## The code

- [`usage/Pricing.java`](../agent/src/main/java/com/example/agent/usage/Pricing.java) — tokens → $ estimate ($0 local)
- [`usage/RunUsage.java`](../agent/src/main/java/com/example/agent/usage/RunUsage.java) — the usage / cost record
- [`chat/AgentService.java`](../agent/src/main/java/com/example/agent/chat/AgentService.java) — reads usage from the final `ChatResponse`
- [`chat/AgentResponse.java`](../agent/src/main/java/com/example/agent/chat/AgentResponse.java) — carries `usage`

## Try it yourself

Every run now reports usage — inspect the `usage` block:

```bash
curl -s localhost:8080/agent/run -H 'Content-Type: application/json' \
  -d '{"input":"Add 100 USD, 50 EUR and 5000 JPY and give the total in GBP."}' \
  | jq .usage
```

`$0` locally; `wallClockMs` grows with loop length. Add a row to `Pricing` to estimate a hosted
model's dollars without changing any other code.

---

→ [Chapter 8 — Letting It Run Detached](08-letting-it-run-detached.md)
