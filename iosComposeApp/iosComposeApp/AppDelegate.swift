import UIKit
import shared

/**
 * AppDelegate for the iOS Compose Multiplatform template app.
 * 
 * Handles iOS app lifecycle events and provides integration points
 * for platform-specific functionality like push notifications,
 * background processing, and system integrations.
 * 
 * Requirements addressed:
 * - 2.4: iOS app lifecycle management
 * - 3.4: Platform-specific app structure with proper lifecycle handling
 */
@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    
    var window: UIWindow?
    
    // MARK: - Application Lifecycle
    
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        // Initialize any iOS-specific services here
        setupLogging()
        
        // Configure app appearance
        configureAppearance()
        
        print("iOS Compose Multiplatform app launched successfully")
        return true
    }
    
    func applicationWillResignActive(_ application: UIApplication) {
        // Sent when the application is about to move from active to inactive state.
        // This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message)
        // or when the user quits the application and it begins the transition to the background state.
        print("App will resign active")
    }
    
    func applicationDidEnterBackground(_ application: UIApplication) {
        // Use this method to release shared resources, save user data, invalidate timers,
        // and store enough application state information to restore your application to its current state
        // in case it is terminated later.
        print("App entered background")
    }
    
    func applicationWillEnterForeground(_ application: UIApplication) {
        // Called as part of the transition from the background to the active state;
        // here you can undo many of the changes made on entering the background.
        print("App will enter foreground")
    }
    
    func applicationDidBecomeActive(_ application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive.
        // If the application was previously in the background, optionally refresh the user interface.
        print("App became active")
    }
    
    func applicationWillTerminate(_ application: UIApplication) {
        // Called when the application is about to terminate.
        // Save data if appropriate. See also applicationDidEnterBackground:.
        print("App will terminate")
    }
    
    // MARK: - UISceneSession Lifecycle (iOS 13+)
    
    @available(iOS 13.0, *)
    func application(
        _ application: UIApplication,
        configurationForConnecting connectingSceneSession: UISceneSession,
        options: UIScene.ConnectionOptions
    ) -> UISceneConfiguration {
        // Called when a new scene session is being created.
        // Use this method to select a configuration to create the new scene with.
        return UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
    }
    
    @available(iOS 13.0, *)
    func application(
        _ application: UIApplication,
        didDiscardSceneSessions sceneSessions: Set<UISceneSession>
    ) {
        // Called when the user discards a scene session.
        // If any sessions were discarded while the application was not running,
        // this will be called shortly after application:didFinishLaunchingWithOptions.
    }
    
    // MARK: - Push Notifications (Optional)
    
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        // Handle successful registration for push notifications
        let tokenParts = deviceToken.map { data in String(format: "%02.2hhx", data) }
        let token = tokenParts.joined()
        print("Device Token: \(token)")
        
        // In a real app, you would send this token to your server
        // or integrate with the transport module for push notification handling
    }
    
    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        // Handle failed registration for push notifications
        print("Failed to register for push notifications: \(error.localizedDescription)")
    }
    
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        // Handle incoming push notifications
        print("Received push notification: \(userInfo)")
        
        // Process the notification and call completion handler
        completionHandler(.newData)
    }
    
    // MARK: - Private Methods
    
    private func setupLogging() {
        // Configure logging for debug builds
        #if DEBUG
        print("Debug logging enabled")
        #endif
    }
    
    private func configureAppearance() {
        // Configure global app appearance
        if #available(iOS 13.0, *) {
            // iOS 13+ appearance configuration
            // The app will use system appearance by default
        } else {
            // iOS 12 and earlier appearance configuration
            UIApplication.shared.statusBarStyle = .default
        }
    }
}

/**
 * SceneDelegate for iOS 13+ scene-based lifecycle management.
 * 
 * Handles scene lifecycle events for apps that support multiple windows
 * and provides proper integration with SwiftUI and Compose.
 */
@available(iOS 13.0, *)
class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    
    var window: UIWindow?
    
    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        // Use this method to optionally configure and attach the UIWindow `window` to the provided UIWindowScene `scene`.
        // If using a storyboard, the `window` property will automatically be initialized and attached to the scene.
        // This delegate does not imply the connecting scene or session are new (see `application:configurationForConnectingSceneSession` instead).
        
        if let windowScene = scene as? UIWindowScene {
            let window = UIWindow(windowScene: windowScene)
            
            // Create and set the root view controller with ContentView
            let contentView = ContentView()
            let hostingController = UIHostingController(rootView: contentView)
            
            window.rootViewController = hostingController
            self.window = window
            window.makeKeyAndVisible()
        }
    }
    
    func sceneDidDisconnect(_ scene: UIScene) {
        // Called as the scene is being released by the system.
        // This occurs shortly after the scene enters the background, or when its session is discarded.
        // Release any resources associated with this scene that can be re-created the next time the scene connects.
        // The scene may re-connect later, as its session was not necessarily discarded (see `application:didDiscardSceneSessions` instead).
    }
    
    func sceneDidBecomeActive(_ scene: UIScene) {
        // Called when the scene has moved from an inactive state to an active state.
        // Use this method to restart any tasks that were paused (or not yet started) when the scene was inactive.
    }
    
    func sceneWillResignActive(_ scene: UIScene) {
        // Called when the scene will move from an active state to an inactive state.
        // This may occur due to temporary interruptions (ex. an incoming phone call).
    }
    
    func sceneWillEnterForeground(_ scene: UIScene) {
        // Called as the scene transitions from the background to the foreground.
        // Use this method to undo the changes made on entering the background.
    }
    
    func sceneDidEnterBackground(_ scene: UIScene) {
        // Called as the scene transitions from the foreground to the background.
        // Use this method to save data, release shared resources, and store enough scene-specific state information
        // to restore the scene back to its current state.
    }
}

// MARK: - SwiftUI Integration

import SwiftUI

/**
 * UIHostingController extension for better integration with Compose Multiplatform.
 */
extension UIHostingController {
    convenience init(rootView: ContentView) {
        self.init(rootView: rootView)
        
        // Configure the hosting controller for optimal Compose integration
        self.view.backgroundColor = UIColor.systemBackground
    }
}