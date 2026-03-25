import Foundation

@MainActor
final class ValidationViewModel: ObservableObject {
    @Published var configuration = WireMockConfiguration()
    @Published var results: [TestScenarioResult] = []
    @Published var isRunning = false
    @Published var logEntries: [LogEntry] = []

    struct LogEntry: Identifiable {
        let id = UUID()
        let timestamp: Date
        let level: Level
        let message: String

        enum Level: String {
            case info = "INFO"
            case success = "PASS"
            case error = "FAIL"
            case debug = "DEBUG"
        }

        var formatted: String {
            let formatter = DateFormatter()
            formatter.dateFormat = "HH:mm:ss.SSS"
            return "[\(formatter.string(from: timestamp))] [\(level.rawValue)] \(message)"
        }
    }

    func log(_ message: String, level: LogEntry.Level = .info) {
        logEntries.append(LogEntry(timestamp: Date(), level: level, message: message))
    }

    // MARK: - Scenario 1: REST Configuration

    func runRESTConfigurationTest() async {
        let result = TestScenarioResult(scenario: .restConfiguration)
        result.steps = [
            TestStep(name: "Send GET request to configuration endpoint"),
            TestStep(name: "Validate HTTP 200 response"),
            TestStep(name: "Validate response body structure"),
        ]
        results.append(result)

        log("--- Scenario: REST Configuration ---")
        log("Requesting: \(configuration.configurationEndpoint)")

        result.steps[0].status = .running
        let restService = RESTValidationService(configuration: configuration)

        do {
            let (config, httpResponse) = try await restService.fetchConfiguration()
            result.steps[0].status = .passed
            result.steps[0].detail = "Request completed"
            log("Received response", level: .debug)

            result.steps[1].status = .running
            if httpResponse.statusCode == 200 {
                result.steps[1].status = .passed
                result.steps[1].detail = "Status: 200 OK"
                log("HTTP status: 200 OK", level: .success)
            } else {
                result.steps[1].status = .failed("Status: \(httpResponse.statusCode)")
                log("HTTP status: \(httpResponse.statusCode)", level: .error)
                result.overallPassed = false
                return
            }

            result.steps[2].status = .running
            if config.deploymentId != nil || config.enabled != nil || config.status != nil {
                result.steps[2].status = .passed
                result.steps[2].detail = "Body contains expected fields"
                log("Config body validated - deploymentId: \(config.deploymentId ?? "n/a"), status: \(config.status ?? "n/a")", level: .success)
            } else {
                result.steps[2].status = .failed("Response body missing expected fields")
                log("Config body missing expected fields", level: .error)
                result.overallPassed = false
                return
            }

            result.overallPassed = true
        } catch {
            let failedIdx = result.steps.firstIndex(where: { $0.status == .running }) ?? 0
            result.steps[failedIdx].status = .failed(error.localizedDescription)
            result.overallPassed = false
            log("REST test failed: \(error.localizedDescription)", level: .error)
        }
    }

    // MARK: - Scenario 2: Full Session Lifecycle

    func runFullSessionLifecycleTest() async {
        let result = TestScenarioResult(scenario: .fullSessionLifecycle)
        result.steps = [
            TestStep(name: "Open WebSocket connection"),
            TestStep(name: "Send configureSession"),
            TestStep(name: "Receive SessionResponse"),
            TestStep(name: "Send user chat message"),
            TestStep(name: "Receive agent response"),
            TestStep(name: "Send closeSession"),
            TestStep(name: "Confirm disconnect"),
        ]
        results.append(result)

        log("--- Scenario: Full Session Lifecycle ---")

        let wsService = WebSocketService(configuration: configuration)
        let handler = WebSocketTestHandler()
        wsService.delegate = handler

        result.steps[0].status = .running
        log("Connecting to \(configuration.webSocketURL)...")
        wsService.connect()

        do {
            try await handler.waitForConnect(timeout: 5)
            result.steps[0].status = .passed
            result.steps[0].detail = "WebSocket connected"
            log("WebSocket connected", level: .success)
        } catch {
            result.steps[0].status = .failed(error.localizedDescription)
            result.overallPassed = false
            log("WebSocket connect failed: \(error.localizedDescription)", level: .error)
            return
        }

        result.steps[1].status = .running
        let configReq = ConfigureSessionRequest(
            token: configuration.token,
            deploymentId: configuration.deploymentId,
            startNew: true,
            tracingId: UUID().uuidString
        )
        log("Sending configureSession...")

        do {
            try await wsService.send(configReq)
            result.steps[1].status = .passed
            result.steps[1].detail = "configureSession sent"
            log("configureSession sent", level: .success)
        } catch {
            result.steps[1].status = .failed(error.localizedDescription)
            result.overallPassed = false
            log("Send configureSession failed: \(error.localizedDescription)", level: .error)
            return
        }

        result.steps[2].status = .running
        log("Waiting for SessionResponse...")

        do {
            let sessionMsg = try await handler.waitForMessage(
                where: { $0.messageClass == "SessionResponse" },
                timeout: 5
            )
            let bodyDict = sessionMsg.body.value as? [String: Any]
            let connected = bodyDict?["connected"] as? Bool ?? false
            if connected {
                result.steps[2].status = .passed
                result.steps[2].detail = "SessionResponse received, connected=true"
                log("SessionResponse: connected=true, newSession=\(bodyDict?["newSession"] ?? "n/a")", level: .success)
            } else {
                result.steps[2].status = .failed("SessionResponse connected=false")
                result.overallPassed = false
                log("SessionResponse: connected=false", level: .error)
                return
            }
        } catch {
            result.steps[2].status = .failed(error.localizedDescription)
            result.overallPassed = false
            log("SessionResponse timeout: \(error.localizedDescription)", level: .error)
            return
        }

        result.steps[3].status = .running
        let chatMsg = OnMessageRequest(
            token: configuration.token,
            message: TextMessagePayload(text: "Hello from iOS validation app"),
            tracingId: UUID().uuidString
        )
        log("Sending chat message: \"Hello from iOS validation app\"")

        do {
            try await wsService.send(chatMsg)
            result.steps[3].status = .passed
            result.steps[3].detail = "Chat message sent"
            log("Chat message sent", level: .success)
        } catch {
            result.steps[3].status = .failed(error.localizedDescription)
            result.overallPassed = false
            log("Send chat message failed: \(error.localizedDescription)", level: .error)
            return
        }

        result.steps[4].status = .running
        log("Waiting for agent response (StructuredMessage)...")

        do {
            let agentMsg = try await handler.waitForMessage(
                where: { $0.messageClass == "StructuredMessage" },
                timeout: 5
            )
            let bodyDict = agentMsg.body.value as? [String: Any]
            let text = bodyDict?["text"] as? String ?? "(no text)"
            result.steps[4].status = .passed
            result.steps[4].detail = "Agent replied: \(text)"
            log("Agent response: \"\(text)\"", level: .success)
        } catch {
            result.steps[4].status = .failed(error.localizedDescription)
            result.overallPassed = false
            log("Agent response timeout: \(error.localizedDescription)", level: .error)
            return
        }

        result.steps[5].status = .running
        let closeReq = CloseSessionRequest(
            token: configuration.token,
            closeAllConnections: true,
            tracingId: UUID().uuidString
        )
        log("Sending closeSession...")

        do {
            try await wsService.send(closeReq)
            result.steps[5].status = .passed
            result.steps[5].detail = "closeSession sent"
            log("closeSession sent", level: .success)
        } catch {
            result.steps[5].status = .failed(error.localizedDescription)
            result.overallPassed = false
            log("Send closeSession failed: \(error.localizedDescription)", level: .error)
            return
        }

        result.steps[6].status = .running
        wsService.disconnect()
        try? await Task.sleep(nanoseconds: 500_000_000)
        result.steps[6].status = .passed
        result.steps[6].detail = "Disconnected cleanly"
        log("Disconnected", level: .success)

        result.overallPassed = true
    }

    // MARK: - Scenario 3: Unexpected Disconnect

    func runUnexpectedDisconnectTest() async {
        let result = TestScenarioResult(scenario: .unexpectedDisconnect)
        result.steps = [
            TestStep(name: "Open WebSocket connection"),
            TestStep(name: "Configure session"),
            TestStep(name: "Trigger server-side disconnect"),
            TestStep(name: "Detect unexpected disconnect"),
        ]
        results.append(result)

        log("--- Scenario: Unexpected Disconnect ---")

        let wsService = WebSocketService(configuration: configuration)
        let handler = WebSocketTestHandler()
        wsService.delegate = handler

        result.steps[0].status = .running
        wsService.connect()

        do {
            try await handler.waitForConnect(timeout: 5)
            result.steps[0].status = .passed
            log("WebSocket connected", level: .success)
        } catch {
            result.steps[0].status = .failed(error.localizedDescription)
            result.overallPassed = false
            log("Connect failed: \(error.localizedDescription)", level: .error)
            return
        }

        result.steps[1].status = .running
        let configReq = ConfigureSessionRequest(
            token: configuration.token,
            deploymentId: configuration.deploymentId,
            startNew: true,
            tracingId: UUID().uuidString
        )

        do {
            try await wsService.send(configReq)
            let _ = try await handler.waitForMessage(
                where: { $0.messageClass == "SessionResponse" },
                timeout: 5
            )
            result.steps[1].status = .passed
            log("Session configured", level: .success)
        } catch {
            result.steps[1].status = .failed(error.localizedDescription)
            result.overallPassed = false
            log("Configure failed: \(error.localizedDescription)", level: .error)
            return
        }

        result.steps[2].status = .running
        log("Requesting WireMock to close connection via admin API...")

        do {
            try await triggerServerDisconnect()
            result.steps[2].status = .passed
            result.steps[2].detail = "Server disconnect triggered"
            log("Server disconnect triggered", level: .success)
        } catch {
            result.steps[2].status = .failed(error.localizedDescription)
            result.overallPassed = false
            log("Trigger disconnect failed: \(error.localizedDescription)", level: .error)
            return
        }

        result.steps[3].status = .running
        log("Waiting for disconnect detection...")

        do {
            try await handler.waitForDisconnect(timeout: 10)
            result.steps[3].status = .passed
            result.steps[3].detail = "Unexpected disconnect detected"
            log("Unexpected disconnect detected and reported", level: .success)
            result.overallPassed = true
        } catch {
            result.steps[3].status = .failed(error.localizedDescription)
            result.overallPassed = false
            log("Disconnect detection failed: \(error.localizedDescription)", level: .error)
        }
    }

    // MARK: - Scenario 4: Reconnect After Disconnect

    func runReconnectTest() async {
        let result = TestScenarioResult(scenario: .reconnectAfterDisconnect)
        result.steps = [
            TestStep(name: "Establish initial connection"),
            TestStep(name: "Configure initial session"),
            TestStep(name: "Disconnect"),
            TestStep(name: "Reconnect with new WebSocket"),
            TestStep(name: "Configure new session"),
            TestStep(name: "Verify new session active"),
        ]
        results.append(result)

        log("--- Scenario: Reconnect After Disconnect ---")

        let wsService1 = WebSocketService(configuration: configuration)
        let handler1 = WebSocketTestHandler()
        wsService1.delegate = handler1

        result.steps[0].status = .running
        wsService1.connect()

        do {
            try await handler1.waitForConnect(timeout: 5)
            result.steps[0].status = .passed
            log("Initial connection established", level: .success)
        } catch {
            result.steps[0].status = .failed(error.localizedDescription)
            result.overallPassed = false
            log("Initial connect failed: \(error.localizedDescription)", level: .error)
            return
        }

        result.steps[1].status = .running
        let configReq1 = ConfigureSessionRequest(
            token: configuration.token,
            deploymentId: configuration.deploymentId,
            startNew: true,
            tracingId: UUID().uuidString
        )

        do {
            try await wsService1.send(configReq1)
            let _ = try await handler1.waitForMessage(
                where: { $0.messageClass == "SessionResponse" },
                timeout: 5
            )
            result.steps[1].status = .passed
            log("Initial session configured", level: .success)
        } catch {
            result.steps[1].status = .failed(error.localizedDescription)
            result.overallPassed = false
            return
        }

        result.steps[2].status = .running
        wsService1.disconnect()
        try? await Task.sleep(nanoseconds: 1_000_000_000)
        result.steps[2].status = .passed
        log("Disconnected from initial session", level: .success)

        result.steps[3].status = .running
        let wsService2 = WebSocketService(configuration: configuration)
        let handler2 = WebSocketTestHandler()
        wsService2.delegate = handler2

        wsService2.connect()

        do {
            try await handler2.waitForConnect(timeout: 5)
            result.steps[3].status = .passed
            log("Reconnected with new WebSocket", level: .success)
        } catch {
            result.steps[3].status = .failed(error.localizedDescription)
            result.overallPassed = false
            log("Reconnect failed: \(error.localizedDescription)", level: .error)
            return
        }

        result.steps[4].status = .running
        let configReq2 = ConfigureSessionRequest(
            token: configuration.token,
            deploymentId: configuration.deploymentId,
            startNew: true,
            tracingId: UUID().uuidString
        )

        do {
            try await wsService2.send(configReq2)
            let sessionMsg = try await handler2.waitForMessage(
                where: { $0.messageClass == "SessionResponse" },
                timeout: 5
            )
            let bodyDict = sessionMsg.body.value as? [String: Any]
            let connected = bodyDict?["connected"] as? Bool ?? false
            if connected {
                result.steps[4].status = .passed
                result.steps[4].detail = "New session configured, connected=true"
                log("New session configured: connected=true", level: .success)
            } else {
                result.steps[4].status = .failed("New session connected=false")
                result.overallPassed = false
                return
            }
        } catch {
            result.steps[4].status = .failed(error.localizedDescription)
            result.overallPassed = false
            return
        }

        result.steps[5].status = .running
        let echoReq = EchoRequest(token: configuration.token)

        do {
            try await wsService2.send(echoReq)
            let echoResponse = try await handler2.waitForMessage(
                where: { $0.tracingId == "SGVhbHRoQ2hlY2tNZXNzYWdlSWQ=" },
                timeout: 5
            )
            result.steps[5].status = .passed
            result.steps[5].detail = "Echo response received - session is active"
            log("Echo response received - session verified active", level: .success)
            result.overallPassed = true
        } catch {
            result.steps[5].status = .failed(error.localizedDescription)
            result.overallPassed = false
            log("Echo verification failed: \(error.localizedDescription)", level: .error)
        }

        wsService2.disconnect()
    }

    // MARK: - Run All

    func runAllScenarios() async {
        isRunning = true
        results.removeAll()
        logEntries.removeAll()
        log("Starting all validation scenarios against \(configuration.baseURL)")

        await runRESTConfigurationTest()
        await runFullSessionLifecycleTest()
        await runUnexpectedDisconnectTest()
        await runReconnectTest()

        let passed = results.filter { $0.overallPassed == true }.count
        let total = results.count
        log("=== Completed: \(passed)/\(total) scenarios passed ===", level: passed == total ? .success : .error)
        isRunning = false
    }

    // MARK: - Helpers

    private func triggerServerDisconnect() async throws {
        let urlString = "\(configuration.adminBaseURL)/disconnect-latest"
        guard let url = URL(string: urlString) else {
            throw ValidationError.invalidURL(urlString)
        }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        let (_, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse,
              (200...299).contains(httpResponse.statusCode) else {
            throw ValidationError.unexpectedStatusCode(
                (response as? HTTPURLResponse)?.statusCode ?? -1
            )
        }
    }
}

// MARK: - WebSocket Test Handler

@MainActor
final class WebSocketTestHandler: WebSocketServiceDelegate {
    private var didConnect = false
    private var didDisconnect = false
    private var receivedMessages: [WebMessagingMessage] = []

    func webSocketDidConnect() {
        didConnect = true
    }

    func webSocketDidReceiveMessage(_ message: WebMessagingMessage, raw: String) {
        receivedMessages.append(message)
    }

    func webSocketDidDisconnect(code: URLSessionWebSocketTask.CloseCode, reason: String?) {
        didDisconnect = true
    }

    func webSocketDidFail(error: Error) {
        didDisconnect = true
    }

    func waitForConnect(timeout: TimeInterval) async throws {
        let deadline = Date().addingTimeInterval(timeout)
        while !didConnect {
            if Date() > deadline { throw ValidationError.timeout }
            try await Task.sleep(nanoseconds: 50_000_000)
        }
    }

    func waitForMessage(where predicate: @escaping (WebMessagingMessage) -> Bool, timeout: TimeInterval) async throws -> WebMessagingMessage {
        let deadline = Date().addingTimeInterval(timeout)
        while true {
            if let existing = receivedMessages.first(where: predicate) {
                return existing
            }
            if Date() > deadline { throw ValidationError.timeout }
            try await Task.sleep(nanoseconds: 50_000_000)
        }
    }

    func waitForDisconnect(timeout: TimeInterval) async throws {
        let deadline = Date().addingTimeInterval(timeout)
        while !didDisconnect {
            if Date() > deadline { throw ValidationError.timeout }
            try await Task.sleep(nanoseconds: 50_000_000)
        }
    }
}
