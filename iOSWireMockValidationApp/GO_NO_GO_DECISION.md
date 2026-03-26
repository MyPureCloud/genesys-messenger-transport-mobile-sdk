# WireMock POC — Go/No-Go Decision

| | |
|---|---|
| **Document Owner** | Shahar Wienrib |
| **Document Version** | 0.2 |
| **Document Status** | Public |
| **Last Update** | 3/26/2026 |

## Objective

Evaluate whether WireMock is viable for mocking the Genesys Web Messaging (Shyrka) protocol to support Messenger SDK UI integration testing on iOS.

## What Was Tested

| Scenario | Result |
|----------|--------|
| REST configuration request mocked via WireMock | PASS |
| Full WebSocket session lifecycle (connect → configure → send → receive → disconnect) | PASS |
| Unexpected server-side disconnect detection | PASS |
| Reconnect after disconnect | PASS |

## Findings

### What Works Well

- **REST mocking (WireMock):** Straightforward JSON stub mapping. Configuration endpoint works out of the box with zero custom code. WireMock's declarative mapping files are easy to maintain and version-control.
- **WebSocket protocol (WireMock 4.x native):** WireMock 4.0.0-beta.29 natively supports WebSocket message stubs via `message-mappings/`. All Shyrka message types used in a typical session — `configureSession`, `onMessage`, `echo`, `closeSession`, `SessionResponse`, `StructuredMessage`, `ConnectionClosedEvent` — were successfully stubbed and validated using declarative JSON with JSON path matching and Handlebars templating. No custom server code required.
- **iOS client (URLSession):** Native `URLSessionWebSocketTask` handles the full lifecycle without any third-party dependencies. ATS local networking exception is sufficient for simulator testing.
- **Single server, single port:** WireMock 4.x serves both REST and WebSocket on the same port (8080). No additional servers, no Node.js, no custom code.
- **Dynamic responses via templating:** WireMock's Handlebars support enables dynamic responses (`{{jsonPath message.body '$.message.text'}}`, `{{now}}`, `{{randomValue}}`), so agent replies echo the user's message text and include realistic timestamps.

### Gaps and Limitations

| Gap | Impact | Mitigation |
|-----|--------|------------|
| **WireMock 4.x is in beta** | Potential API changes; not yet production-stable. | Acceptable for test infrastructure. The WebSocket features work reliably in our testing. |
| **No real WebSocket close** | WireMock sends a `ConnectionClosedEvent` message but doesn't force-close the TCP connection. | Sufficient for UI testing — the SDK reacts to the `ConnectionClosedEvent` payload, not the transport-level close. |
| **Mock fidelity vs. production** | Simplified responses may lack some fields (e.g., `durationSeconds`, richer metadata). | Additional fields can be added to stub JSON as needed. |
| **No TLS/WSS** | Runs over plain HTTP/WS. | Acceptable for local simulator testing. TLS is an infrastructure concern, not a protocol concern. |
| **Single-connection model** | The mock doesn't simulate multi-device session sharing or concurrent connection limits. | Out of scope for initial UI integration tests. Can be added later if needed. |

### Architecture Decision

The POC uses a **single-server approach**:

- **WireMock 4.x** handles both REST endpoints (`wiremock/mappings/`) and WebSocket Shyrka protocol (`wiremock/message-mappings/`) on a single port.

This is the simplest viable architecture — no Node.js, no custom servers, no additional dependencies beyond Java. WireMock 4.x's native WebSocket message stub support (beta) proved reliable for all tested scenarios.

### Maintenance Complexity

| Task | Effort | How |
|------|--------|-----|
| Add new REST endpoint | ~5 min | Add JSON file to `wiremock/mappings/` |
| Add new WebSocket message type | ~10 min | Add JSON file to `wiremock/message-mappings/` with JSON path matcher + response |
| Modify existing response payload | ~2 min | Edit the `data` field in the corresponding JSON stub |
| Add a new test scenario | ~15 min | Add matching stub file + test steps in `ValidationViewModel` |

No compilation, no custom server code, no dependency management. All stubs are version-controlled alongside the project.

### Cross-Platform (iOS, Android, Web)

The WireMock server and all stub files are **identical** across all platforms:

| Component | Reusable Across Platforms? | Notes |
|-----------|---------------------------|-------|
| WireMock JAR | Yes | Single JAR, downloaded once (~24 MB) or fetched by `start-servers.sh` |
| REST stubs (`mappings/`) | Yes | Platform-agnostic JSON |
| WebSocket stubs (`message-mappings/`) | Yes | Platform-agnostic JSON |
| `start-servers.sh` | Yes | Bash script, works on macOS and Linux |
| Test client app | No — one per platform | iOS (this POC), Android (to be created), Web (standard WebSocket API) |

WireMock serves standard HTTP and WebSocket — any client that speaks these protocols can connect. Web test clients can use the browser-native `WebSocket` API or any JS WebSocket library against the same `ws://localhost:8080` endpoint with no changes to the server or stubs.

## Recommendation → GO

WireMock 4.x is viable as the **sole** mock server for Messenger SDK UI integration testing:

1. **Zero custom code** — All mocking is declarative JSON. No Node.js, no custom servers.
2. **Single server** — One WireMock instance handles REST + WebSocket on one port.
3. **Low maintenance** — Adding or modifying stubs is a JSON file edit.
4. **Cross-platform** — Same server and stubs for iOS, Android, and Web.
5. **Already available** — The WireMock 4.x JAR is already in the transport SDK repo.

### Next Steps

1. Expand WebSocket stubs for additional scenarios: typing indicators, attachment flows, session expiry, JWT refresh, too-many-requests throttling.
2. Create an Android test client app using the same WireMock stubs.
3. Integrate into CI pipeline — start WireMock before UI test suite, tear down after.
