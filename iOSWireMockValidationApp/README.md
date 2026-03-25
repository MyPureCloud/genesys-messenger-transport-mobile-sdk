# iOSWireMockValidationApp

**GMMS-15025** — POC: Test Client Validation Against WireMock

A standalone iOS test client that connects to WireMock (REST) and a WebSocket mock server (Shyrka protocol) to validate bidirectional messaging and connection lifecycle for the Messenger SDK.

## Architecture

```
┌──────────────────────────────┐
│  iOSWireMockValidationApp    │
│  (SwiftUI, iOS 17+)         │
│                              │
│  ┌──────────────────────┐    │      ┌──────────────────────┐
│  │ RESTValidationService├────┼─────►│ WireMock (port 8080) │
│  └──────────────────────┘    │      │ REST stubs           │
│                              │      └──────────────────────┘
│  ┌──────────────────────┐    │      ┌──────────────────────┐
│  │ WebSocketService     ├────┼─────►│ WS Mock  (port 8089) │
│  └──────────────────────┘    │      │ Shyrka protocol      │
│                              │      └──────────────────────┘
│  ┌──────────────────────┐    │      ┌──────────────────────┐
│  │ ValidationViewModel  ├────┼─────►│ Admin    (port 8090) │
│  │ (Test orchestrator)  │    │      │ Disconnect control   │
│  └──────────────────────┘    │      └──────────────────────┘
└──────────────────────────────┘
```

## Prerequisites

- **Xcode 15.4+** (Swift 5.10, iOS 17 SDK)
- **Java 11+** (for WireMock standalone)
- **Node.js 18+** (for WebSocket mock server)

## Quick Start

### 1. Start the Mock Servers

```bash
cd iOSWireMockValidationApp
./start-servers.sh
```

This starts:
- **WireMock** on port `8080` (REST configuration stubs)
- **WebSocket mock** on port `8089` (Shyrka protocol)
- **Admin server** on port `8090` (disconnect control for testing)

### 2. Open the iOS App

```bash
open iOSWireMockValidationApp.xcodeproj
```

Build and run on the **iOS Simulator** (iPhone 16 recommended).

### 3. Run Tests

- Tap **"Run All Scenarios"** to execute all four test scenarios
- Or tap individual scenario buttons to run them one at a time
- Switch to the **Logs** tab for detailed protocol-level output

## Configuration

Tap the **gear icon** (⚙️) in the navigation bar to configure:

| Setting | Default | Description |
|---------|---------|-------------|
| Host | `localhost` | Mock server host |
| REST Port | `8080` | WireMock REST port |
| WebSocket Port | `8089` | WebSocket mock server port |
| Admin Port | `8090` | Admin API port |
| Deployment ID | `test-deployment-id` | Deployment ID for config endpoint |
| Token | `test-token-<uuid>` | Session token |

## Test Scenarios

### Scenario 1: REST Configuration Request

**Purpose:** Validate WireMock REST stubs return the expected configuration response.

**Steps:**
1. Sends `GET /api/v2/webmessaging/deployments/{deploymentId}/config`
2. Validates HTTP 200 status
3. Validates response body contains `deploymentId`, `status`, and `enabled` fields

**Pass criteria:** All three steps show green checkmarks.

### Scenario 2: Full Session Lifecycle

**Purpose:** End-to-end WebSocket session: connect → configure → send → receive → disconnect.

**Steps:**
1. Opens WebSocket connection to `ws://localhost:8089/api/v2/webmessaging/messages`
2. Sends `configureSession` request (Shyrka `action: "configureSession"`)
3. Receives `SessionResponse` with `connected: true`
4. Sends a chat message (`action: "onMessage"`, `type: "Text"`)
5. Receives agent response (`class: "StructuredMessage"`, `direction: "Inbound"`)
6. Sends `closeSession` request (`action: "closeSession"`)
7. Confirms WebSocket disconnection

**Pass criteria:** All seven steps show green checkmarks.

### Scenario 3: Unexpected Disconnect Handling

**Purpose:** Verify the client detects when the server closes the connection unexpectedly.

**Steps:**
1. Opens WebSocket connection and configures session
2. Configures session successfully
3. Calls the admin API (`POST /disconnect-latest`) to trigger server-side close
4. Verifies the client detects and reports the unexpected disconnect

**Pass criteria:** All four steps show green checkmarks, especially step 4.

### Scenario 4: Reconnect After Disconnect

**Purpose:** Verify the client can establish a new session after disconnection.

**Steps:**
1. Establishes initial WebSocket connection + session
2. Configures first session
3. Cleanly disconnects
4. Opens a new WebSocket connection
5. Configures a new session (`connected: true`)
6. Sends an `echo` health check and receives response to verify the session is active

**Pass criteria:** All six steps show green checkmarks.

## Project Structure

```
iOSWireMockValidationApp/
├── iOSWireMockValidationApp.xcodeproj/
├── iOSWireMockValidationApp/
│   ├── Info.plist                    # ATS exception for local networking
│   ├── Assets.xcassets/
│   └── Sources/
│       ├── iOSWireMockValidationApp.swift   # App entry point
│       ├── Models/
│       │   ├── ShyrkaMessages.swift          # Shyrka protocol models
│       │   └── TestScenario.swift            # Test scenario types & state
│       ├── Services/
│       │   ├── WireMockConfiguration.swift   # Server config (host/ports)
│       │   ├── RESTValidationService.swift   # REST HTTP client
│       │   └── WebSocketService.swift        # WebSocket client (URLSession)
│       ├── ViewModels/
│       │   └── ValidationViewModel.swift     # Test orchestration logic
│       └── Views/
│           ├── ContentView.swift             # Main UI (tests + logs tabs)
│           └── ConfigurationView.swift       # Settings screen
├── wiremock/
│   ├── mappings/
│   │   ├── configuration-endpoint.json       # REST config stub
│   │   └── admin-scenarios-reset.json        # Admin reset stub
│   ├── websocket-mock-server.js              # Shyrka WebSocket mock
│   └── package.json                          # Node.js dependencies
├── start-servers.sh                          # One-command server startup
└── README.md
```

## Manual Testing Instructions

### Prerequisites Verification

Before running tests, verify your environment:

```bash
# Verify Java is installed (required for WireMock)
java -version

# Verify Node.js is installed (required for WebSocket mock)
node -v

# Verify ports are available
lsof -i :8080  # Should show nothing
lsof -i :8089  # Should show nothing
lsof -i :8090  # Should show nothing
```

### Test Procedure

#### Step 1: Start Mock Servers

```bash
cd /path/to/iOSWireMockValidationApp
./start-servers.sh
```

**Expected output:**
```
All servers running!
  REST (WireMock):    http://localhost:8080
  WebSocket:          ws://localhost:8089/api/v2/webmessaging/messages
  Admin:              http://localhost:8090
```

**Quick verification with curl:**
```bash
# Test REST config endpoint
curl -s http://localhost:8080/api/v2/webmessaging/deployments/test-deployment-id/config | python3 -m json.tool

# Test admin server status
curl -s http://localhost:8090/ | python3 -m json.tool
```

#### Step 2: Run the iOS App

1. Open `iOSWireMockValidationApp.xcodeproj` in Xcode
2. Select an **iPhone Simulator** target (e.g., iPhone 16)
3. Build and Run (⌘R)
4. The app opens showing the **Tests** tab

#### Step 3: Run All Scenarios

1. Tap **"Run All Scenarios"**
2. Watch each scenario execute in sequence
3. Each step shows real-time status:
   - `○` Pending
   - `◉` Running
   - `✓` Passed
   - `✗` Failed

#### Step 4: Verify Individual Scenarios

**Scenario 1 — REST Configuration:**
1. Tap "REST Configuration" button
2. ✓ Step 1: GET request completes
3. ✓ Step 2: HTTP 200 received
4. ✓ Step 3: Response body has expected fields
5. Check **Logs** tab for detailed request/response

**Scenario 2 — Full Session Lifecycle:**
1. Tap "Full Session Lifecycle" button
2. ✓ Step 1: WebSocket connects
3. ✓ Step 2: configureSession sent
4. ✓ Step 3: SessionResponse received (connected=true)
5. ✓ Step 4: Chat message sent
6. ✓ Step 5: Agent response received
7. ✓ Step 6: closeSession sent
8. ✓ Step 7: Disconnect confirmed
9. Verify in Logs tab that agent responded with: `Agent received: "Hello from iOS validation app"`

**Scenario 3 — Unexpected Disconnect:**
1. Tap "Unexpected Disconnect" button
2. ✓ Steps 1-2: Connect and configure
3. ✓ Step 3: Admin API triggers disconnect
4. ✓ Step 4: Client detects the unexpected disconnect
5. Check the WebSocket mock server terminal output for `Force-closing latest connection`

**Scenario 4 — Reconnect After Disconnect:**
1. Tap "Reconnect After Disconnect" button
2. ✓ Steps 1-2: First connection established and configured
3. ✓ Step 3: Disconnect
4. ✓ Step 4: New connection established
5. ✓ Step 5: New session configured (connected=true)
6. ✓ Step 6: Echo health check confirms session is active

#### Step 5: Review Results

- **Tests tab:** Shows pass/fail status for each step in each scenario
- **Logs tab:** Shows timestamped protocol-level messages
- **Terminal:** Shows server-side WebSocket mock logs

### Troubleshooting

| Issue | Solution |
|-------|----------|
| "Request timed out" | Verify mock servers are running (`./start-servers.sh`) |
| "Connection refused" | Check ports aren't in use: `lsof -i :8080,8089,8090` |
| REST test fails | Verify WireMock: `curl http://localhost:8080/api/v2/webmessaging/deployments/test-deployment-id/config` |
| WebSocket fails | Verify WS mock: check Node.js terminal for connection logs |
| Disconnect test fails | Verify admin server: `curl http://localhost:8090/` |
| ATS blocks HTTP | Info.plist has `NSAllowsLocalNetworking = true` (simulator only) |

### Stopping Servers

Press `Ctrl+C` in the terminal where `start-servers.sh` is running. Both servers will be gracefully shut down.
