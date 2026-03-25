import Foundation

@MainActor
protocol WebSocketServiceDelegate: AnyObject {
    func webSocketDidConnect()
    func webSocketDidReceiveMessage(_ message: WebMessagingMessage, raw: String)
    func webSocketDidDisconnect(code: URLSessionWebSocketTask.CloseCode, reason: String?)
    func webSocketDidFail(error: Error)
}

@MainActor
final class WebSocketService: NSObject {
    private let configuration: WireMockConfiguration
    private let urlSession: URLSession
    private var webSocketTask: URLSessionWebSocketTask?
    private var isConnected = false
    weak var delegate: WebSocketServiceDelegate?

    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    init(configuration: WireMockConfiguration) {
        self.configuration = configuration
        let sessionConfig = URLSessionConfiguration.default
        sessionConfig.timeoutIntervalForRequest = 15
        self.urlSession = URLSession(configuration: sessionConfig)
        super.init()
    }

    func connect() {
        guard let url = URL(string: configuration.webSocketURL) else { return }
        let task = urlSession.webSocketTask(with: url)
        self.webSocketTask = task
        task.resume()
        isConnected = true
        delegate?.webSocketDidConnect()
        startReceiving()
    }

    func disconnect() {
        webSocketTask?.cancel(with: .normalClosure, reason: "Client disconnect".data(using: .utf8))
        isConnected = false
    }

    func send<T: Encodable>(_ request: T) async throws {
        let data = try encoder.encode(request)
        guard let jsonString = String(data: data, encoding: .utf8) else {
            throw ValidationError.webSocketError("Failed to encode message")
        }
        try await webSocketTask?.send(.string(jsonString))
    }

    private func startReceiving() {
        webSocketTask?.receive { [weak self] result in
            Task { @MainActor in
                guard let self else { return }
                switch result {
                case .success(let message):
                    self.handleMessage(message)
                    self.startReceiving()
                case .failure(let error):
                    let nsError = error as NSError
                    if nsError.code == 57 || nsError.code == 54 || nsError.code == -1011 {
                        self.isConnected = false
                        self.delegate?.webSocketDidDisconnect(
                            code: .abnormalClosure,
                            reason: error.localizedDescription
                        )
                    } else {
                        self.delegate?.webSocketDidFail(error: error)
                    }
                }
            }
        }
    }

    private func handleMessage(_ message: URLSessionWebSocketTask.Message) {
        switch message {
        case .string(let text):
            guard let data = text.data(using: .utf8) else { return }
            do {
                let decoded = try decoder.decode(WebMessagingMessage.self, from: data)
                delegate?.webSocketDidReceiveMessage(decoded, raw: text)
            } catch {
                delegate?.webSocketDidFail(error: ValidationError.decodingFailed(text))
            }
        case .data:
            break
        @unknown default:
            break
        }
    }

    var connected: Bool { isConnected }
}
