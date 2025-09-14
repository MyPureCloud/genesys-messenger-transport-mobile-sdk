package com.genesys.cloud.messenger.composeapp.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DataModelsTest {

    @Test
    fun testChatMessageCreation() {
        val message = ChatMessage(
            id = "test-id",
            content = "Hello, world!",
            timestamp = 1234567890L,
            isFromUser = true
        )
        
        assertEquals("test-id", message.id)
        assertEquals("Hello, world!", message.content)
        assertEquals(1234567890L, message.timestamp)
        assertTrue(message.isFromUser)
    }

    @Test
    fun testAppStateDefaults() {
        val appState = AppState()
        
        assertFalse(appState.isLoading)
        assertTrue(appState.messages.isEmpty())
        assertEquals(Screen.Home, appState.currentScreen)
        assertEquals(null, appState.error)
    }

    @Test
    fun testAppStateWithData() {
        val messages = listOf(
            ChatMessage("1", "First message", 1000L, true),
            ChatMessage("2", "Second message", 2000L, false)
        )
        
        val appState = AppState(
            isLoading = true,
            messages = messages,
            currentScreen = Screen.Chat,
            error = "Test error"
        )
        
        assertTrue(appState.isLoading)
        assertEquals(2, appState.messages.size)
        assertEquals(Screen.Chat, appState.currentScreen)
        assertEquals("Test error", appState.error)
    }

    @Test
    fun testAppSettingsDefaults() {
        val settings = AppSettings()
        
        assertEquals(ThemeMode.System, settings.theme)
        assertTrue(settings.notifications)
        assertEquals("en", settings.language)
    }

    @Test
    fun testAppSettingsCustom() {
        val settings = AppSettings(
            theme = ThemeMode.Dark,
            notifications = false,
            language = "es"
        )
        
        assertEquals(ThemeMode.Dark, settings.theme)
        assertFalse(settings.notifications)
        assertEquals("es", settings.language)
    }

    @Test
    fun testScreenTypes() {
        val homeScreen = Screen.Home
        val chatScreen = Screen.Chat
        val settingsScreen = Screen.Settings
        
        // Verify they are the correct types
        assertTrue(homeScreen is Screen.Home)
        assertTrue(chatScreen is Screen.Chat)
        assertTrue(settingsScreen is Screen.Settings)
        
        // Verify they are different by checking their class names
        assertFalse(homeScreen::class.equals(chatScreen::class))
        assertFalse(chatScreen::class.equals(settingsScreen::class))
        assertFalse(homeScreen::class.equals(settingsScreen::class))
    }

    @Test
    fun testThemeModeValues() {
        val lightMode = ThemeMode.Light
        val darkMode = ThemeMode.Dark
        val systemMode = ThemeMode.System
        
        // Verify all enum values exist
        assertTrue(lightMode != darkMode)
        assertTrue(darkMode != systemMode)
        assertTrue(lightMode != systemMode)
    }
}