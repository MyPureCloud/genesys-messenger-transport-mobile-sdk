#!/bin/bash
# ============================================================
# Start WireMock + Wrapper for Transport-based UI testing
# ============================================================
# Starts WireMock 4.x with Transport-compatible stubs on port 8080,
# then starts the Python wrapper on port 9090.
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WIREMOCK_DIR="$SCRIPT_DIR/wiremock"
WIREMOCK_PORT=${WIREMOCK_PORT:-8080}
WRAPPER_PORT=${WRAPPER_PORT:-9090}

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

cleanup() {
    echo ""
    echo -e "${YELLOW}Shutting down...${NC}"
    if [ -n "${WRAPPER_PID:-}" ]; then
        kill "$WRAPPER_PID" 2>/dev/null || true
        echo "  Wrapper (PID $WRAPPER_PID) stopped"
    fi
    if [ -n "${WIREMOCK_PID:-}" ]; then
        kill "$WIREMOCK_PID" 2>/dev/null || true
        echo "  WireMock (PID $WIREMOCK_PID) stopped"
    fi
    echo -e "${GREEN}All services stopped.${NC}"
    exit 0
}

trap cleanup SIGINT SIGTERM

# --- Check dependencies ---

if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java is not installed. WireMock requires Java 11+.${NC}"
    echo "Install with: brew install openjdk"
    exit 1
fi

if ! command -v python3 &> /dev/null; then
    echo -e "${RED}Error: Python 3 is not installed.${NC}"
    exit 1
fi

# --- Locate WireMock JAR ---

WIREMOCK_JAR=""

# Check sibling wiremock/ directory (from GMMS-15022 POC)
TRANSPORT_WIREMOCK_DIR="$SCRIPT_DIR/../wiremock"
if [ -f "$TRANSPORT_WIREMOCK_DIR/wiremock-standalone-4.0.0-beta.29.jar" ]; then
    WIREMOCK_JAR="$TRANSPORT_WIREMOCK_DIR/wiremock-standalone-4.0.0-beta.29.jar"
    echo -e "${GREEN}Found WireMock JAR at transport SDK wiremock/ directory${NC}"
fi

# Check iOSWireMockValidationApp (from GMMS-15025 POC)
if [ -z "$WIREMOCK_JAR" ]; then
    WIREMOCK_JAR=$(find "$SCRIPT_DIR/.." -name "wiremock-standalone*.jar" -type f 2>/dev/null | head -1)
fi

if [ -z "$WIREMOCK_JAR" ]; then
    echo -e "${YELLOW}WireMock JAR not found locally. Downloading...${NC}"
    WIREMOCK_JAR="$WIREMOCK_DIR/wiremock-standalone-4.0.0-beta.29.jar"
    curl -fL -o "$WIREMOCK_JAR" \
        "https://repo1.maven.org/maven2/org/wiremock/wiremock-standalone/4.0.0-beta.29/wiremock-standalone-4.0.0-beta.29.jar"
fi

# --- Start WireMock ---

echo ""
echo "============================================================"
echo -e "${GREEN} Starting WireMock on port $WIREMOCK_PORT${NC}"
echo "============================================================"
echo ""

java -jar "$WIREMOCK_JAR" \
    --port "$WIREMOCK_PORT" \
    --root-dir "$WIREMOCK_DIR" \
    --verbose &
WIREMOCK_PID=$!

echo "Waiting for WireMock to be ready..."
retries=0
until curl -s -o /dev/null "http://localhost:${WIREMOCK_PORT}/__admin" 2>/dev/null; do
    retries=$((retries + 1))
    if [ "$retries" -ge 30 ]; then
        echo -e "${RED}ERROR: WireMock did not start within 30 seconds.${NC}"
        exit 1
    fi
    sleep 1
done
echo -e "${GREEN}WireMock is ready.${NC}"

# --- Start Wrapper ---

echo ""
echo "============================================================"
echo -e "${GREEN} Starting Wrapper on port $WRAPPER_PORT${NC}"
echo "============================================================"
echo ""

python3 "$SCRIPT_DIR/wrapper.py" \
    --port "$WRAPPER_PORT" \
    --wiremock-port "$WIREMOCK_PORT" \
    --stubs-dir "$SCRIPT_DIR/stubs" &
WRAPPER_PID=$!

sleep 1

if kill -0 "$WRAPPER_PID" 2>/dev/null; then
    echo ""
    echo "============================================================"
    echo -e "${GREEN} All services running${NC}"
    echo "============================================================"
    echo ""
    echo "  WireMock:    http://localhost:$WIREMOCK_PORT"
    echo "  WebSocket:   ws://localhost:$WIREMOCK_PORT/v1"
    echo "  Wrapper:     http://localhost:$WRAPPER_PORT"
    echo ""
    echo "  Test endpoints:"
    echo "    POST http://localhost:$WRAPPER_PORT/wrapper/send/agentMessage"
    echo "    POST http://localhost:$WRAPPER_PORT/wrapper/send/agentTyping"
    echo "    GET  http://localhost:$WRAPPER_PORT/wrapper/health"
    echo ""
    echo "  Press Ctrl+C to stop all services."
    echo "============================================================"
    echo ""
else
    echo -e "${RED}Wrapper failed to start${NC}"
    cleanup
    exit 1
fi

wait
