package com.genesys.cloud.messenger.composeapp.navigation

import androidx.navigation.NavController
import com.genesys.cloud.messenger.composeapp.model.Screen

/**
 * Extension functions for navigation to provide type-safe navigation
 * between screens using the Screen sealed class.
 */

/**
 * Convert Screen sealed class to navigation route string
 */
fun Screen.toRoute(): String {
    return when (this) {
        is Screen.Home -> "home"
        is Screen.Interaction -> "interaction"
        is Screen.Settings -> "settings"
    }
}

/**
 * Convert navigation route string to Screen sealed class
 */
fun String.toScreen(): Screen {
    return when (this) {
        "home" -> Screen.Home
        "interaction" -> Screen.Interaction
        "settings" -> Screen.Settings
        else -> Screen.Home // Default fallback
    }
}

/**
 * Navigate to a screen using the Screen sealed class
 */
fun NavController.navigateTo(screen: Screen) {
    navigate(screen.toRoute())
}

/**
 * Navigate to a screen with single top behavior (prevents multiple instances)
 */
fun NavController.navigateToSingleTop(screen: Screen) {
    navigate(screen.toRoute()) {
        launchSingleTop = true
    }
}

/**
 * Navigate to a screen and clear the back stack up to a specific destination
 */
fun NavController.navigateAndClearBackStack(screen: Screen, popUpTo: Screen? = null) {
    navigate(screen.toRoute()) {
        popUpTo?.let { destination ->
            popUpTo(destination.toRoute()) {
                inclusive = true
            }
        }
    }
}