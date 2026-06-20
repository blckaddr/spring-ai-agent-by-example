#!/usr/bin/env bash
# Start Ollama-backed agent + both MCP tool servers, wait until ready.
# Logs and PIDs go in ./logs. Stop with scripts/stop-all.sh.
set -euo pipefail
cd "$(dirname "$0")/.."

MODEL="${AGENT_MODEL:-qwen2.5:14b}"
mkdir -p logs

# Ollama reachable?
if ! curl -sf http://localhost:11434/api/version >/dev/null 2>&1; then
  echo "✗ Ollama not reachable at http://localhost:11434"
  echo "  Run 'ollama serve' (or open the app) and 'ollama pull $MODEL', then retry."
  exit 1
fi
echo "✓ Ollama is up (model: $MODEL)"

start() { # name  module  port
  local name=$1 module=$2 port=$3
  if lsof -ti tcp:"$port" -sTCP:LISTEN >/dev/null 2>&1; then echo "• $name already on :$port"; return; fi
  echo "• starting $name on :$port"
  AGENT_MODEL="$MODEL" nohup mvn -q -pl "$module" spring-boot:run > "logs/$name.log" 2>&1 &
  echo $! > "logs/$name.pid"
}

wait_port() { # port
  local port=$1
  for _ in $(seq 1 60); do curl -s -o /dev/null "http://localhost:$port/" && return 0; sleep 1; done
  echo "✗ nothing on :$port after 60s — check logs/"; exit 1
}

# tool servers first (the agent connects to them on startup), then the agent
start mcp-server-currency   mcp-server-currency   8081
start mcp-server-calculator mcp-server-calculator 8082
wait_port 8081; wait_port 8082
start agent agent 8080
wait_port 8080

echo
echo "✓ ready — open http://localhost:8080/"
echo "  logs in ./logs · stop with ./scripts/stop-all.sh"
