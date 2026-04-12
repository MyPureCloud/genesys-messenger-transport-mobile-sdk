# Wrapper Service — WireMock UI Integration Testing

**GMMS-15102** — POC: Wrapper Service Spike

A lightweight Python HTTP wrapper that translates high-level test commands into WireMock Admin API calls, enabling server-initiated events (agent messages, typing indicators) during UI integration tests.

## Architecture

```
┌──────────────────────┐     ┌───────────────────┐     ┌───────────────────────┐
│  XCUITest / Test     │     │  Wrapper Service  │     │  WireMock 4.x         │
│  Runner              │     │  (Python, :9090)  │     │  (:8080)              │
│                      │     │                   │     │                       │
│  POST /wrapper/send/ ├────►│  Load stub JSON   ├────►│  /__admin/channels/   │
│    agentMessage      │     │  Substitute {{}}  │     │    send               │
│                      │     │  Call Admin API   │     │                       │
└──────────────────────┘     └───────────────────┘     │  ┌───────────────┐    │
                                                       │  │ WebSocket /v1 │    │
┌──────────────────────┐                               │  │               │    │
│  iOS App             │◄──────────────────────────────│  │ Push message  │    │
│  (Transport SDK)     │     WebSocket connection      │  └───────────────┘    │
└──────────────────────┘                               └───────────────────────┘
```

## Prerequisites

- **Java 11+** (for WireMock)
- **Python 3** (pre-installed on macOS)
- No additional Python packages required (stdlib only)

## Quick Start

```bash
cd wrapper/
./start-all.sh
```

This starts:
1. WireMock on port `8080` with Transport-compatible stubs
2. Wrapper on port `9090`

Override ports via environment variables:
```bash
WIREMOCK_PORT=8081 WRAPPER_PORT=9091 ./start-all.sh
```

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/wrapper/send/agentMessage` | Send an agent text message to the connected client |
| `POST` | `/wrapper/send/agentTyping` | Send a typing indicator to the connected client |
| `GET`  | `/wrapper/health` | Health check (verifies WireMock reachability) |
| `GET`  | `/wrapper/channels` | List active WireMock WebSocket channels |

### Send Agent Message

```bash
curl -X POST http://localhost:9090/wrapper/send/agentMessage \
  -H "Content-Type: application/json" \
  -d '{"text": "Hello from the agent!"}'
```

Response:
```json
{
  "status": "sent",
  "endpoint": "agentMessage",
  "sentCount": 1,
  "messageId": "a1b2c3d4"
}
```

### Send Agent Typing

```bash
curl -X POST http://localhost:9090/wrapper/send/agentTyping
```

## Directory Layout

```
wrapper/
├── wrapper.py                          # Python HTTP server (stdlib only)
├── start-wrapper.sh                    # Start wrapper standalone
├── start-all.sh                        # Start WireMock + wrapper together
├── README.md
├── stubs/                              # Stub templates with placeholders
│   ├── agentMessage/
│   │   └── v1/stub.json               # {{messageText}}, {{messageId}}, {{timestamp}}
│   └── agentTyping/
│       └── v1/stub.json               # {{messageId}}
└── wiremock/                           # Transport-compatible WireMock config
    ├── mappings/
    │   └── deployment-config.json      # REST: GET /webdeployments/v1/deployments/*/config.json
    └── message-mappings/
        ├── configure-session.json      # WS: configureSession → SessionResponse
        ├── configure-authenticated-session.json
        ├── echo.json                   # WS: echo → StructuredMessage
        ├── on-message-text.json        # WS: onMessage → agent echo reply
        ├── on-message-event.json       # WS: presence events (no response)
        └── close-session.json          # WS: closeSession → ConnectionClosedEvent
```

## How It Works

1. **Test code** calls `POST /wrapper/send/agentMessage` with `{"text": "Hello"}`.
2. **Wrapper** loads `stubs/agentMessage/v1/stub.json`, replaces `{{messageText}}` → `"Hello"`, generates a unique `{{messageId}}` and `{{timestamp}}`.
3. **Wrapper** calls WireMock's `POST /__admin/channels/send` with the rendered Shyrka message, targeting all WebSocket connections on `/v1`.
4. **WireMock** pushes the message to the connected iOS client via the active WebSocket.
5. **Transport SDK** in the iOS app parses the Shyrka `StructuredMessage` and surfaces it in the UI.
6. **XCUITest** asserts the message appears.

## Transport-Compatible Stubs

The WireMock stubs in `wiremock/` are adapted for Transport SDK URL paths:

| URL | Used by | Stub |
|-----|---------|------|
| `ws://localhost:8080/v1?deploymentId=...` | Transport WebSocket | `message-mappings/*.json` |
| `http://localhost:8080/webdeployments/v1/deployments/*/config.json` | Transport deployment config | `mappings/deployment-config.json` |

These differ from the GMMS-15025 validation app stubs (which used `/api/v2/webmessaging/messages` for WebSocket and `/api/v2/webmessaging/deployments/*/config` for REST) because Transport constructs its own URL paths internally.

## Per-Endpoint Stub Versioning

Stubs use a `<endpoint>/<version>/stub.json` folder convention:

```
stubs/
  agentMessage/
    v1/stub.json
    v2/stub.json    ← future: add fields without breaking v1
  agentTyping/
    v1/stub.json
```

Each platform can pin to a specific version independently. Adding a new version requires:
- A new JSON file (`v2/stub.json`)
- No wrapper code changes (the wrapper loads by path)
- No iOS test code changes (unless the test explicitly asserts on the new fields)
