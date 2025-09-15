package com.genesys.cloud.messenger.composeapp

import androidx.compose.ui.window.ComposeUIViewController

/**
 * Creates the main UIViewController for iOS that hosts the Compose Multiplatform UI.
 * 
 * This function serves as the bridge between iOS UIKit and Compose Multiplatform,
 * allowing the shared UI to be displayed in an iOS app.
 * 
 * Requirements addressed:
 * - 2.4: iOS app using shared UI components and ViewModels
 * - 3.4: Platform-specific app structure with shared code integration
 */
fun MainViewController() = ComposeUIViewController {
    App()
}

/**
 * Creates a UIViewController with lifecycle management for iOS.
 * This version provides proper cleanup handling for iOS app lifecycle.
 */
fun MainViewControllerWithLifecycle(
    onViewModelCleared: () -> Unit = {}
) = ComposeUIViewController {
    AppWithLifecycle(
        onViewModelCleared = onViewModelCleared
    )
}