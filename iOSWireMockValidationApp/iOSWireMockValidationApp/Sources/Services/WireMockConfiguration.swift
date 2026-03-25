import Foundation

struct WireMockConfiguration {
    var host: String = "localhost"
    var restPort: Int = 8080
    var webSocketPort: Int = 8089
    var adminPort: Int = 8090
    var deploymentId: String = "test-deployment-id"
    var token: String = "test-token-\(UUID().uuidString)"

    var baseURL: String { "http://\(host):\(restPort)" }
    var webSocketURL: String { "ws://\(host):\(webSocketPort)/api/v2/webmessaging/messages" }
    var configurationEndpoint: String { "\(baseURL)/api/v2/webmessaging/deployments/\(deploymentId)/config" }
    var adminBaseURL: String { "http://\(host):\(adminPort)" }
}
