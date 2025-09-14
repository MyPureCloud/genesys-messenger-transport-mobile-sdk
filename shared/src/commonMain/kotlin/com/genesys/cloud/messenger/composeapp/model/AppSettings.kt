package com.genesys.cloud.messenger.composeapp.model

/**
 * Represents user preferences and app configuration settings.
 *
 * @param theme The selected theme mode for the app
 * @param notifications Whether push notifications are enabled
 * @param language The selected language code (e.g., "en", "es", "fr")
 */
data class AppSettings(
    val theme: ThemeMode = ThemeMode.System,
    val notifications: Boolean = true,
    val language: String = "en"
)

/**
 * Available theme modes for the application.
 */
enum class ThemeMode {
    Light,
    Dark,
    System
}