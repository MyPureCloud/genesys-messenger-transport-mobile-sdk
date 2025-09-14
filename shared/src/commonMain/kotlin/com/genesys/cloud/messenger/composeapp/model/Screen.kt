package com.genesys.cloud.messenger.composeapp.model

/**
 * Sealed class representing the different screens in the application.
 * Used for navigation and state management.
 */
sealed class Screen {
    /**
     * Home screen - main landing page
     */
    object Home : Screen()
    
    /**
     * Chat screen - messaging interface
     */
    object Chat : Screen()
    
    /**
     * Settings screen - app configuration
     */
    object Settings : Screen()
}