# Chapter 5 — When Tools Fail *(Phase 2)*

## What we wanted to learn

Everything so far assumed tools *work*. Real tools don't — they hit bad input, missing data,
timeouts, downed services. For an agent that runs unattended, **what the loop does when a tool
throws** is arguably the most important behavior of all. So we deliberately broke things and
watched.

The good news, which is itself a lesson: we'd already done most of the work. The capture hook
from Phase 0 records errors as readily as results, and we'd *seen* it fire in Phase 1 (the
argument-typing failures). Phase 2 is less "build" and more "probe."

## What we built (almost nothing)

One small improvement, motivated directly by what we observed: **make the captured error
legible.** A failing MCP tool surfaces its error deeply wrapped:

```
ToolExecutionException: Error calling tool: [TextContent[annotations=null,
  text=Unsupported currency code: 'AUD'. Known codes: [...]], meta=null]]
```

The signal — *"Unsupported currency code: 'AUD'"* — is buried in transport noise. We added a few
lines to the hook to pull out the inner message, falling back to the raw text so nothing is ever
lost. After:

```
ToolExecutionException: Unsupported currency code: 'AUD'. Known codes: [CAD, CHF, JPY, EUR, USD, GBP]
```

That's the *entire* code change for this phase. The rest is observation.

### How a failure actually flows

Worth making explicit, because it's the mechanism the whole phase depends on:

1. The tool method throws on the server (our `convert` throws on an unknown currency code).
2. The MCP server returns that as an *error result* (not a crash).
3. The MCP client turns it into a `ToolExecutionException`.
4. Our hook records it into `steps[]` **and rethrows it unchanged**.
5. Spring AI catches it and feeds the error back to the model as the tool's "result."
6. The model gets another turn — and decides what to do about it.

Step 5 is the quiet hero: a tool failure is *not* the end of the loop. It becomes information the
model can react to. Which raises the real question — *how well* does it react?

## What actually happened

We ran these against the strong model from Chapter 4 (`qwen2.5:14b`), and the loop handled
failure in three distinctly different ways.

### 1. It refuses to call a tool it predicts will fail

We asked to convert USD to `ZZZ`. Result: **zero tool calls.** The model knew from its own
knowledge that `ZZZ` isn't a real ISO currency and declined, asking for a valid code. No tool
ever ran.

That's a double-edged lesson. Smart — it avoided a pointless failing call. But also a reminder:
the model sometimes answers from its own knowledge *instead of* using a tool, which is exactly
the silent-failure risk from Chapter 1 wearing a friendly mask. To actually test tool failure we
needed a code the model believes is real but our little demo server doesn't stock — like `AUD`.

### 2. On a real failure, it recovers — using the error message

`convert(100, USD, AUD)` — the model thinks AUD is fine; our server only knows six currencies.
The tool threw, the (now-clean) error went back to the model, and across runs we saw it recover
in two different ways:

- **Apologize and inform:** it listed the supported currencies (taken *verbatim from our error
  message's "Known codes" list*) and offered alternatives.
- **Self-correct and retry:** in another run it picked `CAD` from that same list, called
  `convert(100, USD, CAD) = 136.0`, and returned that, transparently noting the substitution:

  ```
  step 1: convert(USD→AUD)  → ERROR: Unsupported currency code: 'AUD'. Known codes: [CAD, CHF, JPY, EUR, USD, GBP]
  step 2: convert(USD→CAD)  → 136.0
  ```

Crucially, in *neither* case did it invent an AUD exchange rate. Compare that to Chapter 3, where
the 8B model responded to failure by **hallucinating numbers**. Same failure, opposite handling —
and the difference is the model.

### 3. Mid-chain failure: it stops cleanly instead of fabricating

The sternest test: *"Add 100 USD and 50 AUD, total in GBP."* Now a failure lands in the middle
of a dependent chain.

```
step 1: convert(USD→GBP) = 79.0
step 2: convert(AUD→GBP) → ERROR: Unsupported currency code: 'AUD'
```

The model **did not call `add`**, did not fabricate a GBP value for the AUD, and explained that it
could only convert the USD portion (79.0 GBP) and needed AUD data to complete the total. Exactly
the right call — partial honesty over confident nonsense.

> 🐛 **A quirk worth recording.** In that run, qwen2.5:14b delivered its perfectly-correct
> reasoning **in Thai**, unprompted. The *content* was flawless; the *language* drifted. A small
> reminder that models have rough edges unrelated to logic — and, once again, that reading
> `steps[]` (which is language-neutral) beats trusting the prose of the final answer.

## What it taught us

- **A tool failure is information, not a crash — if the plumbing feeds it back.** The single most
  important design choice for resilience is that the loop returns the error to the model and lets
  it take another turn. We got that almost for free from the framework; the hook just had to
  rethrow.
- **Error-message quality is an agent-design concern, not a backend detail.** The model recovered
  *by reading our error text* — it offered and chose alternatives straight from the "Known codes"
  list we happened to include. A vague `"invalid input"` would have left it with nothing to
  recover *with*. Write tool errors for a reader who will act on them, because one will.
- **Recovery quality tracks model quality (again).** The 8B model hallucinated through failures;
  the 14B model apologized, substituted, retried, and refused to fabricate. Chapters 4 and 5 are
  the same lesson from two angles: the model is the reliability ceiling, for success *and* for
  failure.
- **Resilience was mostly free because of decisions made early.** Tools that throw clear,
  content-rich errors + a loop that feeds them back + a hook that captures them = legible,
  recoverable failure, with almost no new code in this phase.

We now have an agent that loops, orchestrates across servers, and copes with failure — but it
still forgets everything the instant a request ends. Next we give it a memory.

## Try it yourself

Ask for a currency the demo server doesn't stock (AUD is a real ISO code, just not in its table):

```bash
curl -s localhost:8080/agent/run -H 'Content-Type: application/json' \
  -d '{"input":"Convert 100 USD to AUD."}'
```

The `convert` step carries an `error`; read how the model recovers — it lists the supported codes
or substitutes one — instead of inventing a rate.

→ [Chapter 6 — Giving It Memory](06-giving-it-memory.md)
