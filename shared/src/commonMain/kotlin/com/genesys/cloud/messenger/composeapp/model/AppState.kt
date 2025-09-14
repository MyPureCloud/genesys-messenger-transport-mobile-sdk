package com.genesys.cloud.messenger.composeapp.model

/**
 * Represents the overall application state.
 *
 * @param isLoading Indicates if the app is currently loading data
 * @param messages List of chat messages in the current conversation
 * @param currentScreen The currently active screen
 * @param error Optional error message to display to the user
 */
data class AppState(
    val isLoading: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val currentScreen: Screen = Screen.Home,
    val error: String? = null
)