import SwiftUI

/**
 * Main iOS App entry point for the Compose Multiplatform template.
 * 
 * This app provides proper integration between SwiftUI and Compose Multiplatform.
 * Uses SwiftUI's built-in lifecycle management for simplicity.
 * 
 * Requirements addressed:
 * - 2.4: iOS app using shared UI components and ViewModels
 * - 3.4: Platform-specific app structure with proper lifecycle handling
 */
@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onAppear {
                    print("iOS Compose Multiplatform app started")
                }
                .onChange(of: scenePhase) { phase in
                    switch phase {
                    case .background:
                        print("App moved to background")
                    case .inactive:
                        print("App became inactive")
                    case .active:
                        print("App became active")
                    @unknown default:
                        print("Unknown scene phase")
                    }
                }
        }
    }
}

/**
 * Alternative SwiftUI-only app structure (commented out).
 * 
 * This version can be used if you prefer pure SwiftUI lifecycle management
 * without AppDelegate. Uncomment and replace the main structure above if needed.
 */
/*
@main
struct iOSAppSwiftUIOnly: App {
    @Environment(\.scenePhase) private var scenePhase
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onChange(of: scenePhase) { phase in
                    switch phase {
                    case .background:
                        print("App moved to background")
                    case .inactive:
                        print("App became inactive")
                    case .active:
                        print("App became active")
                    @unknown default:
                        print("Unknown scene phase")
                    }
                }
        }
    }
}
*/