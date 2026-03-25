import SwiftUI

struct ConfigurationView: View {
    @Binding var configuration: WireMockConfiguration
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Form {
            Section("WireMock Server") {
                HStack {
                    Text("Host")
                    Spacer()
                    TextField("Host", text: $configuration.host)
                        .multilineTextAlignment(.trailing)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }
                HStack {
                    Text("REST Port")
                    Spacer()
                    TextField("REST Port", text: portBinding(\.restPort))
                        .multilineTextAlignment(.trailing)
                        .keyboardType(.numberPad)
                }
                HStack {
                    Text("WebSocket Port")
                    Spacer()
                    TextField("WS Port", text: portBinding(\.webSocketPort))
                        .multilineTextAlignment(.trailing)
                        .keyboardType(.numberPad)
                }
                HStack {
                    Text("Admin Port")
                    Spacer()
                    TextField("Admin Port", text: portBinding(\.adminPort))
                        .multilineTextAlignment(.trailing)
                        .keyboardType(.numberPad)
                }
            }

            Section("Deployment") {
                HStack {
                    Text("Deployment ID")
                    Spacer()
                    TextField("Deployment ID", text: $configuration.deploymentId)
                        .multilineTextAlignment(.trailing)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }
            }

            Section("Session") {
                HStack {
                    Text("Token")
                    Spacer()
                    TextField("Token", text: $configuration.token)
                        .multilineTextAlignment(.trailing)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }
                Button("Generate New Token") {
                    configuration.token = "test-token-\(UUID().uuidString)"
                }
            }

            Section("Computed URLs") {
                VStack(alignment: .leading, spacing: 4) {
                    Text("REST Endpoint")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(configuration.configurationEndpoint)
                        .font(.caption2.monospaced())
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text("WebSocket Endpoint")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(configuration.webSocketURL)
                        .font(.caption2.monospaced())
                }
            }
        }
        .navigationTitle("Configuration")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func portBinding(_ keyPath: WritableKeyPath<WireMockConfiguration, Int>) -> Binding<String> {
        Binding(
            get: { String(configuration[keyPath: keyPath]) },
            set: { configuration[keyPath: keyPath] = Int($0) ?? configuration[keyPath: keyPath] }
        )
    }
}
