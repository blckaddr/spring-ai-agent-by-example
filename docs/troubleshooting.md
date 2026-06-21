# Troubleshooting

Common issues when running the agent locally. See the [root README](../README.md) for setup.

## The agent answers, but `steps[]` is empty (no tools fired)

The model isn't calling tools — it's answering from its own knowledge. Almost always the model
isn't **tool-capable**, or Ollama is too old.

- Use a tool-capable model: `ollama pull qwen2.5:14b` (or a recent `llama3.1`), and set
  `AGENT_MODEL` to it.
- Ensure Ollama ≥ 0.2.8.
- This is a deliberate lesson too — a weak/non-tool model *silently* degrades to "just chatting."

## `Connection refused` / agent fails to reach a tool server

Start order matters: the agent connects to the MCP servers **at startup**.

- Start `mcp-server-currency` (:8081) and `mcp-server-calculator` (:8082) **before** `agent`
  (:8080) — or just use `./scripts/run-all.sh`, which waits for each.
- Check ports aren't already taken: `lsof -i :8080 -i :8081 -i :8082`.

## Ollama not reachable

```
✗ Ollama not reachable at http://localhost:11434
```
Start it: `ollama serve` (or launch the desktop app), then `ollama pull qwen2.5:14b`.

## The answer comes back in another language (Thai, etc.)

A known quirk of `qwen2.5:14b`: it occasionally writes the final answer in another language even
for an English prompt. The tool calls/results are still correct. No prompt instruction reliably
stops it — it's a model trait, not a bug (see book Ch 9). Re-run, or try a different model.

## Redundant tool calls / wrong totals

Smaller models sometimes re-convert amounts or mis-thread results. Mitigations already in place:
the system prompt says "convert each amount exactly once" and "reuse a previously computed result."
A stronger model (e.g. `qwen2.5:14b` vs `llama3.1:8b`) is far more reliable — see book Ch 3–4.

## A run is marked `FAILED` with "max steps reached"

The safety cap (`agent.safety.max-steps`, default 20) tripped — the loop tried too many tool
steps. Raise it in `application.yml` if a legitimate task needs more, or investigate why the model
is looping.

## Push to GitHub rejected: `GH007: Your push would publish a private email address`

Your commits use a private email and GitHub's email-privacy protection blocks it. Either set your
git email to your GitHub **noreply** address (Settings → Emails) and re-commit, or disable the
protection.

## `jq: command not found`

The "Try it yourself" examples pipe JSON to `jq`. Install it: `brew install jq` / `apt install jq`,
or just drop the `| jq` and read the raw JSON.
