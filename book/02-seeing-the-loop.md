# Chapter 2 — Seeing the Loop *(Phase 0)*

## What we wanted to learn

The simplest possible question: **does the tool-calling loop happen at all, and can we see it?**

When you ask an LLM agent "convert 100 USD to EUR," a lot is supposed to happen invisibly: the
model decides it needs a tool, picks one, fills in the arguments, the framework runs it, the
result goes back to the model, and the model writes an answer. Most of that is hidden. Phase 0's
entire goal was to make it *not* hidden.

## What we built

One tool server and one agent.

The tool server (`mcp-server-currency`) exposes two tools. In Spring AI a "tool" is just an
annotated method:

```java
@Tool(description = "Convert an amount of money from one ISO 4217 currency code to another ...")
public double convert(double amount, String from, String to) { ... }
```

That's the whole trick: a plain Java method, plus a description the *model* reads to decide when
to call it. The description is not a comment — it's part of the prompt. The server publishes
these over MCP (Model Context Protocol) so anything speaking MCP can discover and call them.

The agent connects to that server, hands the model the list of available tools, and exposes one
endpoint: `POST /agent/run` with a natural-language request.

### The one habit: make every step visible

The most important thing we built in Phase 0 wasn't the agent — it was a **capture hook**. Every
time the loop calls a tool, we record:

```
{ step, tool, server, arguments, result, error, latencyMs }
```

...log it to the console *and* return it in the API response as a `steps[]` array. We built it
cleanly because every later phase reuses the exact same step data — logged, then chained, then
error-annotated, then persisted, then streamed live. One hook, six phases.

This is the single highest-leverage decision in the whole project. An agent without step capture
is a black box that occasionally lies to you. With it, the box is glass.

## What actually happened

We asked:

```
POST /agent/run
{ "input": "Convert 100 USD to EUR. Use the available tools." }
```

And got back:

```json
{
  "answer": "The conversion of 100 USD to EUR is approximately 92.0 EUR.",
  "steps": [
    {
      "step": 1,
      "tool": "convert",
      "server": "currency",
      "arguments": "{\"amount\":100,\"from\":\"USD\",\"to\":\"EUR\"}",
      "result": "[{\"text\":\"92.0\"}]",
      "error": null,
      "latencyMs": 41
    }
  ]
}
```

There it is. We can see the model *chose* the `convert` tool, *chose* the arguments
(`amount: 100, from: USD, to: EUR` — it parsed those out of the English sentence itself), the
tool returned `92.0`, and the model wrapped that into a sentence. Not a black box — a glass one.

## What it taught us

- **The loop is real and it's the model doing the deciding.** Nothing in our code says "if the
  user mentions currency, call convert." The model read the tool's description and decided. That
  hand-off of control to the model *is* what makes it an agent.
- **The arguments are extracted by the model.** "100", "USD", "EUR" were pulled from a sentence
  into a structured tool call. That's quietly impressive — and, we'd soon learn, quietly fragile.
- **Visibility is a feature you build, not one you get.** The `steps[]` array is why this book can
  show you what happened instead of just asserting it.

One tool, one step, one correct answer. Comfortable. The next phase broke that comfort on purpose
— by asking for something that takes *more than one* step.

## The code

- [`capture/Step.java`](../agent/src/main/java/com/example/agent/capture/Step.java) — the captured-step record (the handrail)
- [`capture/StepCollector.java`](../agent/src/main/java/com/example/agent/capture/StepCollector.java) — accumulates a run's steps
- [`capture/StepCapture.java`](../agent/src/main/java/com/example/agent/capture/StepCapture.java) — binds the collector to the run thread
- [`capture/RecordingToolCallback.java`](../agent/src/main/java/com/example/agent/capture/RecordingToolCallback.java) — the capture hook (wraps each MCP tool)
- [`chat/AgentService.java`](../agent/src/main/java/com/example/agent/chat/AgentService.java) — the tool-calling loop
- [`chat/AgentController.java`](../agent/src/main/java/com/example/agent/chat/AgentController.java) — `POST /agent/run`
- [`mcp-server-currency/…/CurrencyService.java`](../mcp-server-currency/src/main/java/com/example/currency/CurrencyService.java) — the first tool server

## Try it yourself

Start the services ([README](../README.md)), then:

```bash
curl -s localhost:8080/agent/run -H 'Content-Type: application/json' \
  -d '{"input":"Convert 100 USD to EUR."}'
```

Expect `answer` ≈ 92.0 EUR and a single `convert` step in `steps[]` — the loop, made visible.

---

→ [Chapter 3 — The Real Loop](03-the-real-loop.md)
