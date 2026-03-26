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
                    Text("Port")
                    Spacer()
                    TextField("Port", text: portBinding)
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

    private var portBinding: Binding<String> {
        Binding(
            get: { String(configuration.port) },
            set: { configuration.port = Int($0) ?? configuration.port }
        )
    }
}
