#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_PORT="${WRAPPER_PORT:-9090}"
WIREMOCK_PORT="${WIREMOCK_PORT:-8080}"

python3 "$SCRIPT_DIR/wrapper.py" \
  --port "$WRAPPER_PORT" \
  --wiremock-port "$WIREMOCK_PORT" \
  --stubs-dir "$SCRIPT_DIR/stubs"
