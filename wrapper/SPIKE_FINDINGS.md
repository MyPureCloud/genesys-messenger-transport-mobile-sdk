# GMMS-15102 - Wrapper Service Spike Findings

| | |
|---|---|
| **Document Owner** | Shahar Wienrib |
| **Document Version** | 0.1 |
| **Document Status** | Public |
| **Last Update** | April 2026 |

## Objective

Spike a minimal Python wrapper service to answer open design questions raised during grooming, before committing to a full implementation in GMMS-15026.

## What Was Built

| Component | Description |
|-----------|-------------|
| `wrapper.py` | Python stdlib HTTP server (port 9090) with 2 endpoints: `agentMessage`, `agentTyping` |
| Stub templates | Per-endpoint versioned JSON (`stubs/agentMessage/v1/`, `stubs/agentTyping/v1/`) with `{{placeholder}}` substitution |
| Transport-compatible WireMock stubs | REST stub for deployment config, WebSocket stubs for `configureSession`, `autoStart`, `echo`, and presence events |
| iOS XCUITest | `WrapperServiceIntegrationTests.swift` exercising the full flow (3 tests: health, agent message, agent typing) |
| v2 stub | `stubs/agentMessage/v2/stub.json` for impact assessment |

## Acceptance Criteria Results

| AC | Criteria | Status |
|----|----------|--------|
| 1 | Wrapper with 2 endpoints translating to WireMock Admin API | **Met** |
| 2 | iOS test client exercises full flow | **Met** |
| 3 | Stub modification impact assessment | **Met** |
| 4 | Tested against pinned WireMock v4 | **Met** |
| 5 | Recommendations | **Met** |

### AC 1: Wrapper endpoints

The wrapper loads stub JSON templates, substitutes placeholders, and calls WireMock's `POST /__admin/channels/send` to push messages to connected WebSocket clients.

### AC 2: iOS test client

`WrapperServiceIntegrationTests.swift` exercises the full flow: test calls wrapper → wrapper renders stub + calls WireMock Admin API → WireMock pushes Shyrka message via WebSocket → Transport SDK parses it → XCUITest asserts message appears in UI.

### AC 2: Integration issues discovered during runtime validation

Three issues were found and fixed while getting the end-to-end flow working. These are important for GMMS-15026:

| Issue | Root cause | Fix |
|-------|-----------|-----|
| **`sentCount` always 0** | Wrapper read `result.get("sentCount")` but WireMock returns `channelsMessaged` | Changed to `result.get("channelsMessaged", 0)` in `wrapper.py` |
| **Chat UI delayed by ~10s** | Transport SDK sends an `autoStart` request (`action: "onMessage"` with a Presence/Join event) after session configuration. Without a matching stub, the SDK's autostart timer expires before proceeding. | Added `message-mappings/autostart.json` - responds with a `StructuredMessage` containing the expected Presence/Join event |
| **Agent message not found in UI** | XCUITest searched `app.staticTexts` but agent messages render in `UITextView` (shows as `textView` in the accessibility hierarchy) | Changed to `app.descendants(matching: .any).matching(...)` |

**Takeaway for GMMS-15026:** The Transport SDK's WebSocket handshake requires stubs beyond just `configureSession`. Any deployment config with `autoStart.enabled: true` will trigger an additional `onMessage` exchange that WireMock must handle. Future stubs should be derived from the SDK's actual protocol sequences (e.g. capture a real session's WebSocket traffic and create matching WireMock stubs from it).

### AC 4: WireMock v4 stability

All Admin API endpoints used are documented in WireMock's official docs and are not beta-specific:

| Endpoint | Purpose |
|----------|---------|
| `POST /__admin/channels/send` | Push message to WebSocket clients |
| `GET /__admin/channels` | List active WebSocket connections |
| `GET /__admin` | Health check |

---

## Stub Modification Impact Assessment

### Experiment: Adding a `metadata` field to agentMessage

Created `stubs/agentMessage/v2/stub.json` with an additional `metadata` field:

```json
{
  "body": {
    "text": "{{messageText}}",
    ...existing fields...
    "metadata": {
      "priority": "high",
      "source": "wrapper-v2"
    }
  }
}
```

### Impact Analysis

| Component | Change needed? | Why |
|-----------|---------------|-----|
| **Wrapper** | **No** | Blind string substitution - doesn't parse stub structure |
| **Platform tests** (iOS/Android/Web) | **No** | Unless the test explicitly asserts on the new field |
| **Transport SDK** | **No** | `ignoreUnknownKeys` silently drops unknown fields |
| **WireMock** | **No** | Forwards raw JSON without validation |

### What a developer does when a stub needs updating

1. Edit the stub JSON (or create a new version directory).
2. If the new field needs dynamic values, add a `{{placeholder}}` - otherwise no wrapper change.
3. Commit the JSON change - one file, one repo.

**Conclusion:** Stub changes are isolated to JSON files. The wrapper and test code are decoupled from Shyrka schema evolution.

---

## Recommendation 1: Where should wrapper + stubs live?

### Key insight: this is not mobile-specific

The wrapper and stubs are **Shyrka-protocol-level** infrastructure. The Web SDK speaks the same WebSocket protocol as the mobile SDKs, so the web team can reuse the exact same wrapper and stubs (e.g., from Cypress or Playwright) without modification.

### Options evaluated

| Option | Pros | Cons |
|--------|------|------|
| **A. Transport SDK repo** | Close to KMP protocol source; already has WireMock JAR | Web team depends on a mobile-named repo; ownership mismatch |
| **B. Each platform's SDK repo** | Self-contained per platform | 3-way stub duplication (iOS, Android, Web); guaranteed drift |
| **C. Dedicated repo** | Single source of truth for all platforms; clear ownership; independent versioning | Extra repo to maintain; adds a clone step |

### Recommendation: **Option C - Dedicated repo**

Create a lightweight dedicated repository (e.g., `messenger-test-harness`) containing the wrapper, stub templates, and WireMock configuration.

**Rationale:**
- All three SDKs (iOS, Android, Web) share the same protocol - one set of stubs serves all of them.
- Avoids forcing the web team to clone a mobile repo for platform-agnostic infra.
- No duplication, no cross-platform drift.
- Can be versioned independently from any SDK release cycle.

---

## Recommendation 2: Is per-endpoint stub versioning viable?

Folder structure: `stubs/<endpoint>/v1/stub.json`, `v2/stub.json`, etc.

**Tested:** Created `agentMessage/v2/stub.json` with an added `metadata` field. The wrapper loads by file path, so switching versions requires no code change.

**Cross-platform:** Each platform (iOS, Android, Web) can independently pin to its own stub version - the test suite controls which version it requests.

### Current limitation

The wrapper hardcodes `v1`. For GMMS-15026, this should be configurable via query parameter (`?version=2`) or path segment (`/v2/send/agentMessage`).

### Verdict: **Viable**

Adding a new version is a single JSON file - no wrapper or test code changes needed unless a test asserts on new fields.

---

## Summary

| Question | Answer |
|----------|--------|
| Where should wrapper + stubs live? | **Dedicated repo** (long-term), stubs are Shyrka-protocol-level, shared across all platforms (iOS, Android, Web); Transport SDK repo is acceptable as a short-term starting point |
| Is per-endpoint stub versioning viable? | **Yes**, JSON-only changes, independent per platform, no code changes for new versions |
| Does the wrapper need changes when a stub is modified? | **No**, unless a new `{{placeholder}}` is introduced |
| Do platform tests need changes when a stub is modified? | **No**, unless a test asserts on the changed field (applies equally to iOS, Android, and Web) |
| Is WireMock's Admin API stable enough? | **Yes**, `/__admin/channels/send` is documented, not beta-specific |

### Next Steps

1. **GMMS-15026:** Full wrapper implementation - version parameter, error handling, CI integration.
2. **GMMS-15028 / GMMS-15037:** Integrate WireMock + wrapper into Jenkins pipelines (iOS on EC2 Mac, Android in Docker). Note: pipeline tickets should be updated to include starting the wrapper service alongside WireMock, and to use the corrected Transport-compatible stubs from `wrapper/wiremock/`.
3. Migrate wrapper + stubs to a dedicated repo before web team onboarding.
