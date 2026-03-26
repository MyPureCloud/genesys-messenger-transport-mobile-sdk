# iOSWireMockValidationApp

**GMMS-15025** — POC: Test Client Validation Against WireMock

A standalone iOS test client that connects to WireMock 4.x for both REST and WebSocket (Shyrka protocol) to validate bidirectional messaging and connection lifecycle for the Messenger SDK.

## Architecture

```
┌──────────────────────────────┐
│  iOSWireMockValidationApp    │
│  (SwiftUI, iOS 17+)         │
│                              │
│  ┌──────────────────────┐    │      ┌──────────────────────────────┐
│  │ RESTValidationService├────┼─────►│                              │
│  └──────────────────────┘    │      │   WireMock 4.x (port 8080)  │
│                              │      │                              │
│  ┌──────────────────────┐    │      │   REST stubs (mappings/)     │
│  │ WebSocketService     ├────┼─────►│   + WebSocket message stubs  │
│  └──────────────────────┘    │      │     (message-mappings/)      │
│                              │      │                              │
│  ┌──────────────────────┐    │      │   Single server, single port │
│  │ ValidationViewModel  │    │      └──────────────────────────────┘
│  │ (Test orchestrator)  │    │
│  └──────────────────────┘    │
└──────────────────────────────┘
```

**Key:** WireMock 4.x natively supports WebSocket message stubs, so a single server handles both REST and WebSocket — no Node.js or custom mock servers needed.

## Prerequisites

- **Xcode 15.4+** (Swift 6.0, iOS 17 SDK)
- **Java 11+** (for WireMock standalone)

That's it — no Node.js required.

## Quick Start

### 1. Start WireMock

```bash
cd iOSWireMockValidationApp
./start-servers.sh
```

This starts WireMock on port `8080` serving both REST stubs and WebSocket message stubs.

### 2. Open the iOS App

```bash
open iOSWireMockValidationApp.xcodeproj
```

Build and run on the **iOS Simulator**.

### 3. Run Tests

- Tap **"Run All Scenarios"** to execute all four test scenarios
- Or tap individual scenario buttons to run them one at a time
- Switch to the **Logs** tab for detailed protocol-level output

## Configuration

Tap the **gear icon** in the navigation bar to configure:

| Setting | Default | Description |
|---------|---------|-------------|
| Host | `localhost` | WireMock server host |
| Port | `8080` | WireMock port (REST + WebSocket) |
| Deployment ID | `test-deployment-id` | Deployment ID for config endpoint |
| Token | `test-token-<uuid>` | Session token |

## Test Scenarios

### Scenario 1: REST Configuration Request

Validates WireMock REST stubs return the expected configuration response.

**Steps:** GET config endpoint → validate HTTP 200 → validate response body fields.

### Scenario 2: Full Session Lifecycle

End-to-end WebSocket session: connect → configureSession → send message → receive agent response → closeSession → disconnect.

**Steps:**
1. Open WebSocket to `ws://localhost:8080/api/v2/webmessaging/messages`
2. Send `configureSession` → receive `SessionResponse` (connected=true)
3. Send chat message → receive `StructuredMessage` agent reply
4. Send `closeSession` → confirm disconnect

### Scenario 3: Unexpected Disconnect Handling

Verifies the client detects a server-initiated disconnect.

**Steps:** Connect + configure → send `triggerDisconnect` (test helper action) → receive `ConnectionClosedEvent` → verify detection.

### Scenario 4: Reconnect After Disconnect

Verifies the client can establish a new session after disconnection.

**Steps:** Connect + configure → disconnect → reconnect → configure new session → verify via echo health check.

## WireMock Stubs

### REST Stubs (`wiremock/mappings/`)

| File | Endpoint | Description |
|------|----------|-------------|
| `configuration-endpoint.json` | `GET /api/v2/webmessaging/deployments/*/config` | Deployment configuration |

### WebSocket Message Stubs (`wiremock/message-mappings/`)

| File | Trigger (action) | Response |
|------|-------------------|----------|
| `configure-session.json` | `configureSession` | `SessionResponse` (connected=true) |
| `configure-authenticated-session.json` | `configureAuthenticatedSession` | `SessionResponse` (connected=true) |
| `on-message-text.json` | `onMessage` (type=Text) | `StructuredMessage` (agent reply with templated text) |
| `on-message-event.json` | `onMessage` (type=Event) | No response (presence event acknowledged) |
| `echo.json` | `echo` | `StructuredMessage` (ping, with tracingId) |
| `close-session.json` | `closeSession` | `SessionResponse` (connected=false) + `ConnectionClosedEvent` |
| `trigger-disconnect.json` | `triggerDisconnect` | `ConnectionClosedEvent` (test helper) |

All stubs use WireMock's native Handlebars templating for dynamic values (`{{jsonPath message.body '$.field'}}`, `{{now}}`, `{{randomValue}}`).

## Project Structure

```
iOSWireMockValidationApp/
├── iOSWireMockValidationApp.xcodeproj/
├── iOSWireMockValidationApp/
│   ├── Info.plist
│   ├── Assets.xcassets/
│   └── Sources/
│       ├── iOSWireMockValidationApp.swift
│       ├── Models/
│       │   ├── ShyrkaMessages.swift
│       │   └── TestScenario.swift
│       ├── Services/
│       │   ├── WireMockConfiguration.swift
│       │   ├── RESTValidationService.swift
│       │   └── WebSocketService.swift
│       ├── ViewModels/
│       │   └── ValidationViewModel.swift
│       └── Views/
│           ├── ContentView.swift
│           └── ConfigurationView.swift
├── wiremock/
│   ├── mappings/
│   │   └── configuration-endpoint.json
│   └── message-mappings/
│       ├── configure-session.json
│       ├── configure-authenticated-session.json
│       ├── on-message-text.json
│       ├── on-message-event.json
│       ├── echo.json
│       ├── close-session.json
│       └── trigger-disconnect.json
├── start-servers.sh
├── GO_NO_GO_DECISION.md
└── README.md
```

## Manual Testing Instructions

### Prerequisites

```bash
java -version    # Java 11+ required
lsof -i :8080    # Port should be available
```

### Step 1: Start WireMock

```bash
./start-servers.sh
```

Verify: `curl -s http://localhost:8080/api/v2/webmessaging/deployments/test-deployment-id/config | python3 -m json.tool`

### Step 2: Build & Run the iOS App

Open in Xcode → select iPhone Simulator → Cmd+R.

### Step 3: Run & Verify Scenarios

Tap **"Run All Scenarios"**. All four scenarios should show green checkmarks. Check the **Logs** tab for protocol-level detail.

### Stopping

Press `Ctrl+C` in the terminal running `start-servers.sh`.

### Troubleshooting

| Issue | Solution |
|-------|----------|
| "Request timed out" | Verify WireMock is running (`./start-servers.sh`) |
| "Connection refused" | Check port: `lsof -i :8080` |
| REST test fails | `curl http://localhost:8080/api/v2/webmessaging/deployments/test-deployment-id/config` |
| WebSocket fails | Check WireMock terminal for connection logs |
