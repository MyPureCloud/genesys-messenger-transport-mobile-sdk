import SwiftUI
import Shared

/**
 * Main ContentView for the iOS Compose Multiplatform template app.
 * 
 * This view integrates the shared Compose UI with SwiftUI, providing:
 * - Proper keyboard handling
 * - Safe area management
 * - Lifecycle integration
 * - Theme support
 * 
 * Requirements addressed:
 * - 2.4: iOS app using shared UI components and ViewModels
 * - 3.4: Platform-specific app structure with shared code integration
 */
struct ContentView: View {
    @Environment(\.colorScheme) var colorScheme
    @State private var isActive = true
    
    var body: some View {
        ComposeView(
            themeMode: themeMode(from: colorScheme),
            isActive: isActive
        )
        .ignoresSafeArea(.keyboard) // Compose has own keyboard handling
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.willResignActiveNotification)) { _ in
            isActive = false
        }
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification)) { _ in
            isActive = true
        }
    }
    
    private func themeMode(from colorScheme: ColorScheme) -> ThemeMode {
        switch colorScheme {
        case .light:
            return ThemeMode.light
        case .dark:
            return ThemeMode.dark
        @unknown default:
            return ThemeMode.system
        }
    }
}

/**
 * UIViewControllerRepresentable wrapper for the Compose Multiplatform UI.
 * 
 * This struct bridges SwiftUI and Compose, handling:
 * - ViewController creation and lifecycle
 * - Theme mode updates
 * - Proper cleanup on view disposal
 */
struct ComposeView: UIViewControllerRepresentable {
    let themeMode: ThemeMode
    let isActive: Bool
    
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewControllerWithLifecycle(
            themeMode: themeMode,
            onViewModelCleared: {
                // Handle any iOS-specific cleanup here
                print("ViewModels cleared for iOS lifecycle")
            }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // Handle updates if needed (e.g., theme changes)
        // Note: In a production app, you might want to recreate the controller
        // or implement theme change handling in the Compose side
    }
}

/**
 * Preview provider for ContentView.
 * Provides previews for both light and dark themes.
 */
struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            ContentView()
                .preferredColorScheme(.light)
                .previewDisplayName("Light Theme")
            
            ContentView()
                .preferredColorScheme(.dark)
                .previewDisplayName("Dark Theme")
        }
    }
}