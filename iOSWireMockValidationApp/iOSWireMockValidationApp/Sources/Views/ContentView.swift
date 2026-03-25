import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = ValidationViewModel()
    @State private var selectedTab = 0

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Picker("View", selection: $selectedTab) {
                    Text("Tests").tag(0)
                    Text("Logs").tag(1)
                }
                .pickerStyle(.segmented)
                .padding()

                if selectedTab == 0 {
                    testsView
                } else {
                    logsView
                }
            }
            .navigationTitle("WireMock Validation")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    configButton
                }
            }
        }
    }

    private var testsView: some View {
        VStack {
            configurationSummary
            scenarioButtons
            resultsList
        }
    }

    private var configurationSummary: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Image(systemName: "server.rack")
                    .foregroundStyle(.secondary)
                Text("WireMock: \(viewModel.configuration.baseURL)")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal)
    }

    private var scenarioButtons: some View {
        VStack(spacing: 8) {
            Button {
                Task { await viewModel.runAllScenarios() }
            } label: {
                HStack {
                    Image(systemName: "play.fill")
                    Text("Run All Scenarios")
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
            }
            .buttonStyle(.borderedProminent)
            .disabled(viewModel.isRunning)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(TestScenarioType.allCases) { scenario in
                        Button {
                            Task { await runSingle(scenario) }
                        } label: {
                            Text(scenario.rawValue)
                                .font(.caption)
                                .lineLimit(1)
                        }
                        .buttonStyle(.bordered)
                        .disabled(viewModel.isRunning)
                    }
                }
            }
        }
        .padding(.horizontal)
        .padding(.bottom, 8)
    }

    @ViewBuilder
    private var resultsList: some View {
        if viewModel.results.isEmpty {
            ContentUnavailableView(
                "No Results Yet",
                systemImage: "checkmark.circle.badge.questionmark",
                description: Text("Run a scenario to see results")
            )
        } else {
            List {
                ForEach(viewModel.results.reversed()) { result in
                    ScenarioResultView(result: result)
                }
            }
            .listStyle(.insetGrouped)
        }
    }

    private var logsView: some View {
        Group {
            if viewModel.logEntries.isEmpty {
                ContentUnavailableView(
                    "No Logs",
                    systemImage: "doc.text",
                    description: Text("Logs will appear when tests run")
                )
            } else {
                List {
                    ForEach(viewModel.logEntries.reversed()) { entry in
                        Text(entry.formatted)
                            .font(.system(.caption, design: .monospaced))
                            .foregroundStyle(color(for: entry.level))
                    }
                }
                .listStyle(.plain)
            }
        }
    }

    private var configButton: some View {
        NavigationLink {
            ConfigurationView(configuration: $viewModel.configuration)
        } label: {
            Image(systemName: "gear")
        }
    }

    private func runSingle(_ scenario: TestScenarioType) async {
        viewModel.isRunning = true
        switch scenario {
        case .restConfiguration:
            await viewModel.runRESTConfigurationTest()
        case .fullSessionLifecycle:
            await viewModel.runFullSessionLifecycleTest()
        case .unexpectedDisconnect:
            await viewModel.runUnexpectedDisconnectTest()
        case .reconnectAfterDisconnect:
            await viewModel.runReconnectTest()
        }
        viewModel.isRunning = false
    }

    private func color(for level: ValidationViewModel.LogEntry.Level) -> Color {
        switch level {
        case .info: return .primary
        case .success: return .green
        case .error: return .red
        case .debug: return .secondary
        }
    }
}

// MARK: - Scenario Result View

struct ScenarioResultView: View {
    @ObservedObject var result: TestScenarioResult

    var body: some View {
        Section {
            ForEach(result.steps) { step in
                HStack {
                    Text(step.status.emoji)
                        .foregroundStyle(stepColor(step.status))
                        .font(.system(.body, design: .monospaced))
                        .frame(width: 24)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(step.name)
                            .font(.subheadline)
                        if !step.detail.isEmpty {
                            Text(step.detail)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        if case .failed(let msg) = step.status {
                            Text(msg)
                                .font(.caption)
                                .foregroundStyle(.red)
                        }
                    }
                }
            }
        } header: {
            HStack {
                Text(result.scenario.rawValue)
                    .font(.headline)
                Spacer()
                overallBadge
            }
        }
    }

    @ViewBuilder
    private var overallBadge: some View {
        if let passed = result.overallPassed {
            Text(passed ? "PASSED" : "FAILED")
                .font(.caption.bold())
                .padding(.horizontal, 8)
                .padding(.vertical, 2)
                .background(passed ? Color.green.opacity(0.2) : Color.red.opacity(0.2))
                .foregroundStyle(passed ? .green : .red)
                .clipShape(Capsule())
        } else {
            ProgressView()
                .controlSize(.small)
        }
    }

    private func stepColor(_ status: StepStatus) -> Color {
        switch status {
        case .pending: return .gray
        case .running: return .blue
        case .passed: return .green
        case .failed: return .red
        }
    }
}
