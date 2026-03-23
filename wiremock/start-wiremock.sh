#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WIREMOCK_VERSION="4.0.0-beta.29"
WIREMOCK_JAR="wiremock-standalone-${WIREMOCK_VERSION}.jar"
WIREMOCK_URL="https://repo1.maven.org/maven2/org/wiremock/wiremock-standalone/${WIREMOCK_VERSION}/${WIREMOCK_JAR}"
PORT="${1:-8080}"

cd "$SCRIPT_DIR"

if [ ! -f "$WIREMOCK_JAR" ]; then
  echo "Downloading WireMock standalone v${WIREMOCK_VERSION}..."
  curl -fLo "$WIREMOCK_JAR" "$WIREMOCK_URL"
  echo "Download complete."
fi

cleanup() {
  if [ -n "${WIREMOCK_PID:-}" ]; then
    echo "Stopping WireMock (PID: ${WIREMOCK_PID})..."
    kill "$WIREMOCK_PID" 2>/dev/null || true
    wait "$WIREMOCK_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

echo "Starting WireMock on port ${PORT}..."
java -jar "$WIREMOCK_JAR" \
  --port "$PORT" \
  --root-dir "$SCRIPT_DIR" \
  --verbose &
WIREMOCK_PID=$!

echo "Waiting for WireMock to be ready..."
retries=0
until curl -s -o /dev/null "http://localhost:${PORT}/__admin" 2>/dev/null; do
  retries=$((retries + 1))
  if [ "$retries" -ge 30 ]; then
    echo "ERROR: WireMock did not start within 30 seconds."
    exit 1
  fi
  sleep 1
done
echo "WireMock is ready."

echo ""
echo "Loading message stubs..."
bash "${SCRIPT_DIR}/load-stubs.sh" "$PORT"

echo ""
echo "WireMock is running on port ${PORT}. WebSocket URL: ws://localhost:${PORT}/v1"
echo "Press Ctrl+C to stop."
wait "$WIREMOCK_PID"
