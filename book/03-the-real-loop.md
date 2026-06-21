# Chapter 3 — The Real Loop *(Phase 1)*

## What we wanted to learn

Phase 0's task was a single tool call. That's barely a "loop." We wanted a task that **genuinely
requires several dependent steps** — where the agent must call tools *in sequence*, and later
calls depend on earlier results. That's where "agent" stops being a fancy word for "function
call" and starts being real.

We also wanted two tool *servers*, not one, so we could watch the agent aggregate tools from
multiple sources and orchestrate across them.

## What we built

A second tool server, `mcp-server-calculator` (port 8082), exposing `add`, `subtract`,
`multiply`. The agent now connects to **both** servers; their tools merge into one toolset the
model can draw from. Adding the second server was almost nothing — one connection entry in
config — which is itself a lesson: the architecture (agent separate from tools, reached over a
protocol) made growth cheap.

We made one deliberate design choice that *is* the lesson of this chapter: we kept the tools
**granular**. We did **not** build a single `convertAndSum` tool that does everything. We wanted
the *model* to figure out the sequence: convert each amount, then add the results. If we'd baked
the orchestration into one coarse tool, there'd be no orchestration left for the agent to do —
and no lesson.

We also upgraded the capture hook to record *which server* each tool came from, by asking each
connection for its name and tool list at startup:

```
MCP tool->server index: {add=calculator-tools, subtract=calculator-tools,
                         convert=currency-tools, multiply=calculator-tools,
                         listRates=currency-tools}
```

## The task

> *"Add 100 USD, 50 EUR and 5000 JPY together and give me the total in GBP."*

Think about what this requires. You can't add 100 + 50 + 5000 — they're different currencies.
The agent must:

1. `convert` 100 USD → GBP
2. `convert` 50 EUR → GBP
3. `convert` 5000 JPY → GBP
4. `add` the three results

Four steps, across two servers, where step 4 depends on steps 1–3. A real loop. The correct
answer, for the record, is **147.09 GBP**.

## What actually happened

This is where the book stops being a success story and becomes useful.

We ran it with `llama3.1:8b` — a capable, popular 8-billion-parameter local model. Over several
runs, the model's behavior peeled apart into **three distinct layers of failure**, each one only
visible *because* of the step-capture hook. Watch them fall in order.

### Layer 1 — Wrong order

On the first run, with no special instructions, the model called `add` *first* — on the raw,
unconverted amounts:

```
step 1: add(numbers=[100, 50, 5000]) = 5150     ← adding dollars + euros + yen. Nonsense.
step 2: convert(100 USD → GBP) = 79.0
step 3: convert(50 EUR → GBP) = 42.93
step 4: convert(5000 JPY → GBP) = 25.16
```

It added before it converted, then did the *final* sum silently in its own head. The answer
happened to come out right (147.09) — but only by luck of correct mental arithmetic. The
*process* was wrong.

We addressed this the way you're supposed to: a **system prompt** telling the model to convert
each amount first and only then add. That fixed the ordering. Onto the next layer.

### Layer 2 — Wrong types (and a confident lie)

With the ordering fixed, a new failure surfaced. The model now called the tools in the right
order — but passed its arguments as the wrong JSON *type*: strings instead of numbers.

```
step 1: convert(amount="100", ...) → ERROR: "string found, number expected"
step 2: convert(amount="50",  ...) → ERROR: "string found, number expected"
step 3: convert(amount="5000",...) → ERROR: "string found, number expected"
step 4: add(numbers="[1.0, 2.0, 3.0]") → ERROR: "string found, array expected"
```

Every single tool call was *rejected* by the tool server's input validation. And here's the part
worth tattooing on your arm: the model's final answer was still confident and plausible —

> *"100 USD is equal to 76.31 GBP... the total in GBP is 161.05."*

Every number in that sentence was **hallucinated**. Not one tool call had succeeded. If we'd
only looked at the `answer` field — the way a normal app would — it looked like a tidy success.
The `steps[]` told the truth: total failure, papered over with invented numbers.

We fixed the typing too, with an explicit instruction to emit JSON numbers, not strings.

### Layer 3 — Wrong data flow (the one prompting couldn't fix)

Now the structure was finally perfect. Right order, right types, all four tool calls *succeeding*:

```
step 1: convert(amount=100, USD→GBP) = 79.0     ✓
step 2: convert(amount=50,  EUR→GBP) = 42.93    ✓
step 3: convert(amount=5000,JPY→GBP) = 25.16    ✓
step 4: add(numbers=[23.32, 21.91, 184.19]) = 229.42
```

Look at step 4. The converts returned **79.0, 42.93, 25.16**. The model fed `add` the numbers
**23.32, 21.91, 184.19** — values it *made up*. It did not carry its own tool results forward
into the next call.

We ran it three more times to be sure it wasn't a fluke:

| Run | What `add` was given | Result | (correct: 147.09) |
|----:|----------------------|-------:|-------------------|
| 1 | [23.32, 21.91, 184.19] | 229.42 | ✗ |
| 2 | [23.32, 21.91, 183.19] | 228.42 | ✗ |
| 3 | [23.99, 25.49, 5083.95] | 5133.43 | ✗ |

Consistent. The model **reliably produced the correct shape** of the loop and **reliably failed
to thread the data through it.** And no amount of prompting fixed layer 3 — because it isn't a
prompting problem. It's a *capability* problem. An 8B model can plan the steps but can't reliably
hold "the number I just got back" in mind while constructing the next call.

## What it taught us

- **The capture hook earned its entire existence here.** Three times, the `answer` field looked
  fine or plausible while the actual process was broken. Without `steps[]`, we'd have shipped an
  agent that confidently lies. *Observe the process, never trust just the output.*
- **Failures come in layers, and prompting reaches only some of them.** Order and argument-typing
  yielded to better instructions. Data-threading did not. Knowing *which* layer you're fighting
  tells you whether to reach for a better prompt or a better model.
- **The model is a component with a capability ceiling — and it's often the bottleneck.** Our
  code was correct. The tools were correct. The orchestration *structure* was correct. The thing
  that was wrong was the model's reasoning, and you can't refactor your way around that.
- **This is why "which model" was a config property from day one.** We built the experiment in
  before we knew we'd need it.

Which leaves an obvious, testable question hanging in the air:

> If the 8B model gets the *shape* right but can't thread the *data*, will a bigger, stronger
> model — same code, same prompts, just swap the brain — actually solve it?

That's the next chapter.

## The code

- [`mcp-server-calculator/…/CalculatorService.java`](../mcp-server-calculator/src/main/java/com/example/calculator/CalculatorService.java) — the second tool server (add/subtract/multiply)
- [`mcp/McpToolServerIndex.java`](../agent/src/main/java/com/example/agent/mcp/McpToolServerIndex.java) — tool → server attribution (now that there are two servers)
- [`chat/AgentService.java`](../agent/src/main/java/com/example/agent/chat/AgentService.java) — the system prompt that steers convert→add ordering

## Try it yourself

```bash
curl -s localhost:8080/agent/run -H 'Content-Type: application/json' \
  -d '{"input":"Add 100 USD, 50 EUR and 5000 JPY and give the total in GBP."}'
```

Watch `steps[]`: three `convert` calls (currency-tools) then one `add` (calculator-tools) → 147.09.
On `qwen2.5:14b` it threads the values; on `llama3.1:8b` it often won't (next chapter).

---

→ [Chapter 4 — A Better Brain](04-a-better-brain.md)
