# WireMock v4 – Shyrka WebSocket Stubs

Local WireMock instance for mocking Shyrka WebSocket flows used in iOS UI integration tests.

## Prerequisites

- **Java 17+** (`java -version`)
- **curl** (pre-installed on macOS)
- **websocat** or **wscat** for interactive WebSocket validation (optional):
  ```bash
  brew install websocat
  # or
  npm install -g wscat
  ```

## Quick Start

```bash
cd wiremock/
./start-wiremock.sh
```

The script:
1. Downloads the WireMock 4.0.0-beta.29 standalone JAR on first run
2. Starts WireMock on port 8080 (override with `./start-wiremock.sh 9090`)
3. Waits for the server to be ready
4. Loads all message stubs from `messages/` via the admin API
5. Keeps the server running in the foreground (Ctrl+C to stop)

To reload stubs on a running instance without restarting:

```bash
./load-stubs.sh
```

## Directory Layout

```
wiremock/
├── start-wiremock.sh              # Download + launch + load stubs
├── load-stubs.sh                  # Load message stubs via admin API
├── messages/                      # WebSocket message stub definitions
│   ├── session-connect.json       # configureSession / configureAuthenticatedSession
│   ├── message-echo.json          # echo (health check)
│   ├── message-send-receive.json  # onMessage (text send/receive)
│   └── disconnect.json            # closeSession
└── __files/responses/             # Response body payloads
    ├── session-response.json
    ├── structured-message-inbound.json
    ├── echo-response.json
    └── connection-closed-event.json
```

## Stub Summary

| Stub File | Matches `action` | Response |
|-----------|-----------------|----------|
| `session-connect.json` | `configureSession`, `configureAuthenticatedSession` | `SessionResponse` (connected, new session) |
| `message-echo.json` | `echo` | `StructuredMessage` echo reply |
| `message-send-receive.json` | `onMessage` | `StructuredMessage` (inbound agent text) |
| `disconnect.json` | `closeSession` | `ConnectionClosedEvent` |

## Validation

### 1. Verify WireMock is running

```bash
curl -s http://localhost:8080/__admin/message-mappings | python3 -m json.tool
```

Expected: 5 message mappings listed.

### 2. Test WebSocket stubs with websocat

**Session connect:**
```bash
echo '{"action":"configureSession","token":"test-token","deploymentId":"test-deployment","startNew":false,"tracingId":"test-tracing-id"}' \
  | websocat ws://localhost:8080/v1
```

Expected: `SessionResponse` with `connected: true`, `newSession: true`.

**Message send/receive:**
```bash
echo '{"action":"onMessage","token":"test-token","message":{"text":"Hello world","type":"Text"},"tracingId":"test-tracing-id"}' \
  | websocat ws://localhost:8080/v1
```

Expected: `StructuredMessage` with `text: "Hello from the agent!"`.

**Echo (health check):**
```bash
echo '{"action":"echo","token":"test-token","tracingId":"SGVhbHRoQ2hlY2tNZXNzYWdlSWQ=","message":{"text":"ping","type":"Text"}}' \
  | websocat ws://localhost:8080/v1
```

Expected: `StructuredMessage` echo with `text: "ping"`.

**Disconnect:**
```bash
echo '{"action":"closeSession","token":"test-token","closeAllConnections":true,"tracingId":"test-tracing-id"}' \
  | websocat ws://localhost:8080/v1
```

Expected: `ConnectionClosedEvent`.

### 3. Interactive testing with wscat

```bash
wscat -c ws://localhost:8080/v1
```

Paste any of the JSON payloads above to see the stub responses interactively.

## Shyrka Protocol Reference

- **Client requests** use `action` + `token` + `tracingId` (flat JSON objects)
- **Server responses** use `type` + `class` + `code` + `body` (envelope format)
- WebSocket URL: `wss://webmessaging.<domain>/v1?deploymentId=<id>&application=<app>`
- Response body files match the structures in `transport/src/androidUnitTest/kotlin/transport/util/Response.kt`
