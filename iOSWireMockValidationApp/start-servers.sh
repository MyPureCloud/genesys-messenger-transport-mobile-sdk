#!/bin/bash

# ============================================================
# WireMock Server Startup Script
# ============================================================
# Starts WireMock 4.x with REST stubs + WebSocket message stubs
# on a single port. No Node.js or external dependencies needed.
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WIREMOCK_DIR="$SCRIPT_DIR/wiremock"
WIREMOCK_PORT=${WIREMOCK_PORT:-8080}

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

cleanup() {
    echo ""
    echo -e "${YELLOW}Shutting down WireMock...${NC}"
    if [ -n "$WIREMOCK_PID" ]; then
        kill "$WIREMOCK_PID" 2>/dev/null || true
        echo "  WireMock (PID $WIREMOCK_PID) stopped"
    fi
    echo -e "${GREEN}Server stopped.${NC}"
    exit 0
}

trap cleanup SIGINT SIGTERM

# --- Check dependencies ---

if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java is not installed. WireMock requires Java 11+.${NC}"
    echo "Install with: brew install openjdk"
    exit 1
fi

# --- Locate WireMock JAR ---

WIREMOCK_JAR=""
TRANSPORT_SDK_DIR="$SCRIPT_DIR/../wiremock"

if [ -f "$TRANSPORT_SDK_DIR/wiremock-standalone-4.0.0-beta.29.jar" ]; then
    WIREMOCK_JAR="$TRANSPORT_SDK_DIR/wiremock-standalone-4.0.0-beta.29.jar"
    echo -e "${GREEN}Found WireMock JAR at transport SDK location${NC}"
else
    WIREMOCK_JAR=$(find "$SCRIPT_DIR/.." -name "wiremock-standalone*.jar" -type f 2>/dev/null | head -1)
    if [ -z "$WIREMOCK_JAR" ]; then
        echo -e "${YELLOW}WireMock JAR not found locally. Downloading...${NC}"
        WIREMOCK_JAR="$WIREMOCK_DIR/wiremock-standalone.jar"
        curl -L -o "$WIREMOCK_JAR" "https://repo1.maven.org/maven2/org/wiremock/wiremock-standalone/4.0.0-beta.29/wiremock-standalone-4.0.0-beta.29.jar"
    fi
fi

echo ""
echo "============================================================"
echo -e "${GREEN} Starting WireMock Server${NC}"
echo "============================================================"
echo ""

java -jar "$WIREMOCK_JAR" \
    --port "$WIREMOCK_PORT" \
    --root-dir "$WIREMOCK_DIR" \
    --verbose &
WIREMOCK_PID=$!

sleep 3

if kill -0 "$WIREMOCK_PID" 2>/dev/null; then
    echo ""
    echo "============================================================"
    echo -e "${GREEN} WireMock running on port $WIREMOCK_PORT${NC}"
    echo "============================================================"
    echo ""
    echo "  REST + WebSocket:   http://localhost:$WIREMOCK_PORT"
    echo "  WebSocket:          ws://localhost:$WIREMOCK_PORT/api/v2/webmessaging/messages"
    echo "  Config endpoint:    http://localhost:$WIREMOCK_PORT/api/v2/webmessaging/deployments/test-deployment-id/config"
    echo ""
    echo "  REST stubs:         wiremock/mappings/"
    echo "  WebSocket stubs:    wiremock/message-mappings/"
    echo ""
    echo "  Press Ctrl+C to stop"
    echo "============================================================"
    echo ""
else
    echo -e "${RED}WireMock failed to start${NC}"
    exit 1
fi

wait
