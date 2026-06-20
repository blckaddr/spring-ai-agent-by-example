#!/usr/bin/env bash
# Stop the agent + both MCP tool servers (by port). Leaves Ollama running.
set -uo pipefail
cd "$(dirname "$0")/.."

for port in 8080 8081 8082; do
  pid=$(lsof -ti tcp:"$port" -sTCP:LISTEN 2>/dev/null || true)
  if [ -n "$pid" ]; then
    kill $pid 2>/dev/null && echo "• stopped :$port (pid $pid)"
  else
    echo "• nothing on :$port"
  fi
done
