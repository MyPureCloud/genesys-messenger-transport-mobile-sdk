import Foundation

enum TestScenarioType: String, CaseIterable, Identifiable {
    case restConfiguration = "REST Configuration"
    case fullSessionLifecycle = "Full Session Lifecycle"
    case unexpectedDisconnect = "Unexpected Disconnect"
    case reconnectAfterDisconnect = "Reconnect After Disconnect"

    var id: String { rawValue }

    var description: String {
        switch self {
        case .restConfiguration:
            return "Validates that WireMock REST stubs return the expected configuration response."
        case .fullSessionLifecycle:
            return "Full WebSocket lifecycle: connect → configure → send message → receive agent response → disconnect."
        case .unexpectedDisconnect:
            return "Detects when WireMock closes the WebSocket connection unexpectedly."
        case .reconnectAfterDisconnect:
            return "Reconnects after a disconnect and establishes a new WebSocket session."
        }
    }
}

enum StepStatus: Equatable {
    case pending
    case running
    case passed
    case failed(String)

    var emoji: String {
        switch self {
        case .pending: return "○"
        case .running: return "◉"
        case .passed: return "✓"
        case .failed: return "✗"
        }
    }
}

struct TestStep: Identifiable {
    let id = UUID()
    let name: String
    var status: StepStatus = .pending
    var detail: String = ""
}

@MainActor
final class TestScenarioResult: ObservableObject, Identifiable {
    let id = UUID()
    let scenario: TestScenarioType
    @Published var steps: [TestStep] = []
    @Published var overallPassed: Bool?

    init(scenario: TestScenarioType) {
        self.scenario = scenario
    }
}
