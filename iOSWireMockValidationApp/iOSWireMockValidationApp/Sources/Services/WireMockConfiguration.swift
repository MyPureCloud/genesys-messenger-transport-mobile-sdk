import Foundation

struct WireMockConfiguration {
    var host: String = "localhost"
    var port: Int = 8080
    var deploymentId: String = "test-deployment-id"
    var token: String = "test-token-\(UUID().uuidString)"

    var baseURL: String { "http://\(host):\(port)" }
    var webSocketURL: String { "ws://\(host):\(port)/api/v2/webmessaging/messages" }
    var configurationEndpoint: String { "\(baseURL)/api/v2/webmessaging/deployments/\(deploymentId)/config" }
}
