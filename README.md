# Building an Agent, One Concept at a Time

A learning project: a [Spring AI](https://spring.io/projects/spring-ai) agent that calls tools
over **MCP**, built in small phases so you can *see* how an agentic loop actually works — including
where it breaks. It runs entirely **locally** on a free model via [Ollama](https://ollama.com); no
API keys, no cost.

> 📖 **Read the book:** **<https://blckaddr.github.io/spring-ai-agent-by-example/>** — a
> chapter-by-chapter narrative of building this, one concept at a time (start with its
> Introduction). Source in [`book/`](book/README.md). This README is just how to **run** it.

## What's inside

| Module | Port | Role |
|--------|------|------|
| `agent` | 8080 | the orchestrator — Ollama ChatClient + MCP client + REST/SSE endpoints |
| `mcp-server-currency` | 8081 | a tool server — `convert()`, `listRates()` |
| `mcp-server-calculator` | 8082 | a tool server — `add()`, `subtract()`, `multiply()` |
| `mcp-server-feestax` | 8083 | a tool server — `transactionFee()`, `taxRate()` (used by the Phase-6 planning page) |

The agent reaches the tools **only over MCP/HTTP**, never by importing their code — same as a real
deployment. Stack: Spring AI 2.0 + Spring Boot 4 + Java 21.

## Prerequisites

- **Java 21** and **Maven 3.9+**
- **[`jq`](https://jqlang.github.io/jq/)** — for the JSON in the examples (`brew install jq` /
  `apt install jq`)
- **[Ollama](https://ollama.com)** plus a **tool-capable** model:
  ```bash
  ollama pull qwen2.5:14b      # recommended — reliable multi-step tool use (~10 GB RAM)
  ```
  `llama3.1:8b` also works but is noticeably less reliable at multi-step tasks — which is itself a
  lesson (see book Chapters 3–4).

## Run it

1. **Start Ollama:** `ollama serve` (or launch the desktop app).
2. **Start the four services** — tool servers first, then the agent (it connects to them on
   startup). Either run the helper script:
   ```bash
   ./scripts/run-all.sh        # starts all four, waits until ready; logs in ./logs
   ./scripts/stop-all.sh       # stops them
   ```
   …or run each in its own terminal:
   ```bash
   mvn -pl mcp-server-currency   spring-boot:run        # :8081
   mvn -pl mcp-server-calculator spring-boot:run        # :8082
   mvn -pl mcp-server-feestax    spring-boot:run        # :8083
   AGENT_MODEL=qwen2.5:14b mvn -pl agent spring-boot:run # :8080
   ```
3. **Open the hub:** <http://localhost:8080/> — a landing page linking to **Watch it think**
   (the chat, `/chat`) and **Watch it plan** (the planning graph, `/plan`, Phase 6).

## Smoke test (60 seconds)

```bash
curl -s localhost:8080/agent/run -H 'Content-Type: application/json' \
  -d '{"input":"Convert 100 USD to EUR."}' | jq
```
You should get an answer of ~92.0 EUR and a `steps[]` array showing the `convert` tool being
called. If you see an answer but **no** steps, your model probably can't call tools — pull a
tool-capable one (above).

## The model is a config knob

The model is set by the `AGENT_MODEL` env var (default in config: `llama3.1:8b`). Swap it to see
how much the *model* drives reliability:
```bash
AGENT_MODEL=qwen2.5:3b mvn -pl agent spring-boot:run   # weaker — watch it struggle
```

## Repo layout

- [`book/`](book/README.md) — the narrative (read this)
- [`plans/`](plans/) — the phased build plan
- [`docs/adr/`](docs/adr/) — architecture decisions · [`docs/architecture.md`](docs/architecture.md) — how it fits together
- [`NOTES.md`](NOTES.md) — running lab notebook · [`CLAUDE.md`](CLAUDE.md) — build guidance

## Configuration knobs (agent)

| Property | Default | Meaning |
|----------|---------|---------|
| `AGENT_MODEL` | `llama3.1:8b` | Ollama model id (set to `qwen2.5:14b`) |
| `agent.async.pool-size` | 4 | background workers for detached runs |
| `agent.safety.max-steps` | 20 | hard cap on tool steps per run |
| `agent.safety.max-wall-clock-ms` | 120000 | detached-run timeout |
