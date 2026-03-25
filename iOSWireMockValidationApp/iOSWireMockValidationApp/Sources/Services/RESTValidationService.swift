import Foundation

actor RESTValidationService {
    private let configuration: WireMockConfiguration
    private let session: URLSession

    init(configuration: WireMockConfiguration) {
        self.configuration = configuration
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 10
        self.session = URLSession(configuration: config)
    }

    struct ConfigurationResponse: Decodable {
        let enabled: Bool?
        let deploymentId: String?
        let status: String?
        let messenger: MessengerConfig?
    }

    struct MessengerConfig: Decodable {
        let enabled: Bool?
    }

    func fetchConfiguration() async throws -> (ConfigurationResponse, HTTPURLResponse) {
        guard let url = URL(string: configuration.configurationEndpoint) else {
            throw ValidationError.invalidURL(configuration.configurationEndpoint)
        }

        let (data, response) = try await session.data(from: url)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw ValidationError.notHTTPResponse
        }

        let decoded = try JSONDecoder().decode(ConfigurationResponse.self, from: data)
        return (decoded, httpResponse)
    }
}

enum ValidationError: LocalizedError {
    case invalidURL(String)
    case notHTTPResponse
    case unexpectedStatusCode(Int)
    case timeout
    case decodingFailed(String)
    case webSocketError(String)
    case disconnected(String)

    var errorDescription: String? {
        switch self {
        case .invalidURL(let url): return "Invalid URL: \(url)"
        case .notHTTPResponse: return "Response is not HTTP"
        case .unexpectedStatusCode(let code): return "Unexpected status code: \(code)"
        case .timeout: return "Request timed out"
        case .decodingFailed(let msg): return "Decoding failed: \(msg)"
        case .webSocketError(let msg): return "WebSocket error: \(msg)"
        case .disconnected(let reason): return "Disconnected: \(reason)"
        }
    }
}
