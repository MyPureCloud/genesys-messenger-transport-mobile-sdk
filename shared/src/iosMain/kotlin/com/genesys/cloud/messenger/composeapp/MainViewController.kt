package com.genesys.cloud.messenger.composeapp

import androidx.compose.ui.window.ComposeUIViewController
import com.genesys.cloud.messenger.composeapp.model.ThemeMode

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
    App(themeMode = ThemeMode.System)
}

/**
 * Creates a UIViewController with custom theme mode for iOS.
 * This allows iOS-specific theme configuration while using shared UI.
 */
fun MainViewController(themeMode: ThemeMode) = ComposeUIViewController {
    App(themeMode = themeMode)
}

/**
 * Creates a UIViewController with lifecycle management for iOS.
 * This version provides proper cleanup handling for iOS app lifecycle.
 */
fun MainViewControllerWithLifecycle(
    themeMode: ThemeMode = ThemeMode.System,
    onViewModelCleared: () -> Unit = {}
) = ComposeUIViewController {
    AppWithLifecycle(
        themeMode = themeMode,
        onViewModelCleared = onViewModelCleared
    )
}