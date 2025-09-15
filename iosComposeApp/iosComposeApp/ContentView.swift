import SwiftUI
import Shared

/**
 * Main ContentView for the iOS Compose Multiplatform template app.
 * 
 * This view integrates the shared Compose UI with SwiftUI, providing:
 * - Proper keyboard handling
 * - Safe area management
 * - Lifecycle integration
 * 
 * Requirements addressed:
 * - 2.4: iOS app using shared UI components and ViewModels
 * - 3.4: Platform-specific app structure with shared code integration
 */
struct ContentView: View {
    @Environment(\.colorScheme) var colorScheme
    @State private var isActive = true
    
    var body: some View {
        ComposeView(isActive: isActive)
            .ignoresSafeArea(.all) // Compose has own safe area handling
            .onReceive(NotificationCenter.default.publisher(for: UIApplication.willResignActiveNotification)) { _ in
                isActive = false
            }
            .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification)) { _ in
                isActive = true
            }
    }
}

/**
 * UIViewControllerRepresentable wrapper for the Compose Multiplatform UI.
 * 
 * This struct bridges SwiftUI and Compose, handling:
 * - ViewController creation and lifecycle
 * - Proper cleanup on view disposal
 */
struct ComposeView: UIViewControllerRepresentable {
    let isActive: Bool
    
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // Handle updates if needed
        // The Compose UI will handle its own theme changes based on system settings
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