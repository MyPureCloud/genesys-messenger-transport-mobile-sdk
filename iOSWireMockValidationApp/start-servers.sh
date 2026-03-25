#!/bin/bash

# ============================================================
# WireMock + WebSocket Mock Server Startup Script
# ============================================================
# This script starts:
#   1. WireMock (REST stubs on port 8080)
#   2. WebSocket mock server (Shyrka protocol on port 8089)
#   3. Admin server (disconnect control on port 8090)
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WIREMOCK_DIR="$SCRIPT_DIR/wiremock"
WIREMOCK_PORT=${WIREMOCK_PORT:-8080}
WS_PORT=${WS_PORT:-8089}
ADMIN_PORT=${ADMIN_PORT:-8090}

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

cleanup() {
    echo ""
    echo -e "${YELLOW}Shutting down servers...${NC}"
    if [ -n "$WIREMOCK_PID" ]; then
        kill "$WIREMOCK_PID" 2>/dev/null || true
        echo "  WireMock (PID $WIREMOCK_PID) stopped"
    fi
    if [ -n "$WS_PID" ]; then
        kill "$WS_PID" 2>/dev/null || true
        echo "  WebSocket mock (PID $WS_PID) stopped"
    fi
    echo -e "${GREEN}All servers stopped.${NC}"
    exit 0
}

trap cleanup SIGINT SIGTERM

# --- Check dependencies ---

if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java is not installed. WireMock requires Java.${NC}"
    echo "Install with: brew install openjdk"
    exit 1
fi

if ! command -v node &> /dev/null; then
    echo -e "${RED}Error: Node.js is not installed. WebSocket mock requires Node.js.${NC}"
    echo "Install with: brew install node"
    exit 1
fi

# --- Install Node dependencies ---

echo -e "${YELLOW}Installing Node.js dependencies...${NC}"
cd "$WIREMOCK_DIR"
npm install --silent
cd "$SCRIPT_DIR"

# --- Locate WireMock JAR ---

WIREMOCK_JAR=""
TRANSPORT_SDK_DIR="$SCRIPT_DIR/../genesys-messenger-transport-mobile-sdk/wiremock"

if [ -f "$TRANSPORT_SDK_DIR/wiremock-standalone-4.0.0-beta.29.jar" ]; then
    WIREMOCK_JAR="$TRANSPORT_SDK_DIR/wiremock-standalone-4.0.0-beta.29.jar"
    echo -e "${GREEN}Found WireMock JAR at transport SDK location${NC}"
else
    WIREMOCK_JAR=$(find "$SCRIPT_DIR/.." -name "wiremock-standalone*.jar" -type f 2>/dev/null | head -1)
    if [ -z "$WIREMOCK_JAR" ]; then
        echo -e "${YELLOW}WireMock JAR not found locally. Downloading...${NC}"
        WIREMOCK_JAR="$WIREMOCK_DIR/wiremock-standalone.jar"
        curl -L -o "$WIREMOCK_JAR" "https://repo1.maven.org/maven2/org/wiremock/wiremock-standalone/3.9.2/wiremock-standalone-3.9.2.jar"
    fi
fi

echo ""
echo "============================================================"
echo -e "${GREEN} Starting WireMock Validation Servers${NC}"
echo "============================================================"
echo ""

# --- Start WireMock ---

echo -e "${YELLOW}Starting WireMock on port $WIREMOCK_PORT...${NC}"
java -jar "$WIREMOCK_JAR" \
    --port "$WIREMOCK_PORT" \
    --root-dir "$WIREMOCK_DIR" \
    --verbose &
WIREMOCK_PID=$!

sleep 3

if kill -0 "$WIREMOCK_PID" 2>/dev/null; then
    echo -e "${GREEN}WireMock started (PID $WIREMOCK_PID)${NC}"
else
    echo -e "${RED}WireMock failed to start${NC}"
    exit 1
fi

# --- Start WebSocket mock ---

echo -e "${YELLOW}Starting WebSocket mock server on port $WS_PORT...${NC}"
WS_PORT=$WS_PORT ADMIN_PORT=$ADMIN_PORT node "$WIREMOCK_DIR/websocket-mock-server.js" &
WS_PID=$!

sleep 1

if kill -0 "$WS_PID" 2>/dev/null; then
    echo -e "${GREEN}WebSocket mock started (PID $WS_PID)${NC}"
else
    echo -e "${RED}WebSocket mock failed to start${NC}"
    cleanup
    exit 1
fi

echo ""
echo "============================================================"
echo -e "${GREEN} All servers running!${NC}"
echo "============================================================"
echo ""
echo "  REST (WireMock):    http://localhost:$WIREMOCK_PORT"
echo "  WebSocket:          ws://localhost:$WS_PORT/api/v2/webmessaging/messages"
echo "  Admin:              http://localhost:$ADMIN_PORT"
echo ""
echo "  Config endpoint:    http://localhost:$WIREMOCK_PORT/api/v2/webmessaging/deployments/test-deployment-id/config"
echo ""
echo "  Press Ctrl+C to stop all servers"
echo "============================================================"
echo ""

wait
