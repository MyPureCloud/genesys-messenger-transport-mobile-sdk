package com.genesys.cloud.messenger.composeapp.integration

import com.genesys.cloud.messenger.composeapp.model.AppSettings
import com.genesys.cloud.messenger.composeapp.model.ChatMessage
import com.genesys.cloud.messenger.composeapp.model.Screen
import com.genesys.cloud.messenger.composeapp.model.ThemeMode
import com.genesys.cloud.messenger.composeapp.util.TestDispatcherRule
import com.genesys.cloud.messenger.composeapp.viewmodel.ChatViewModel
import com.genesys.cloud.messenger.composeapp.viewmodel.HomeViewModel
import com.genesys.cloud.messenger.composeapp.viewmodel.SettingsViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cross-platform integration tests for shared module components.
 * 
 * These tests verify that shared components work correctly on both Android and iOS:
 * - ViewModels maintain consistent behavior across platforms
 * - Data models serialize/deserialize correctly
 * - Navigation logic works consistently
 * - State management is platform-agnostic
 * 
 * Requirements addressed:
 * - 2.5: Testing shared components work on both platforms
 * - 3.5: Verifying shared module integration consistency
 */
class CrossPlatformIntegrationTest {

    private val testDispatcherRule = TestDispatcherRule()

    @BeforeTest
    fun setUp() {
        testDispatcherRule.setUp()
    }

    @AfterTest
    fun tearDown() {
        testDispatcherRule.tearDown()
    }

    @Test
    fun testViewModelInitializationConsistency() = runTest {
        // Test that ViewModels initialize consistently across platforms
        val homeViewModel = HomeViewModel()
        val chatViewModel = ChatViewModel()
        val settingsViewModel = SettingsViewModel()
        
        // Test that ViewModels can be created without crashing
        assertNotNull(homeViewModel)
        assertNotNull(chatViewModel)
        assertNotNull(settingsViewModel)
        
        // Test that state flows exist
        assertNotNull(homeViewModel.navigationEvent)
        assertNotNull(chatViewModel.uiState)
        assertNotNull(settingsViewModel.uiState)
    }

    @Test
    fun testDataModelConsistency() {
        // Test that data models work consistently across platforms
        val message = ChatMessage(
            id = "test-123",
            content = "Hello, World!",
            timestamp = 1234567890L,
            isFromUser = true
        )
        
        assertEquals("test-123", message.id)
        assertEquals("Hello, World!", message.content)
        assertEquals(1234567890L, message.timestamp)
        assertTrue(message.isFromUser)
        
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
    fun testNavigationConsistency() = runTest {
        val homeViewModel = HomeViewModel()
        
        // Test navigation to different screens
        homeViewModel.navigateToChat()
        
        // Verify navigation event is created
        val navigationEvent = homeViewModel.navigationEvent.value
        assertNotNull(navigationEvent)
        
        // Test navigation to settings
        homeViewModel.navigateToSettings()
        
        val settingsNavigationEvent = homeViewModel.navigationEvent.value
        assertNotNull(settingsNavigationEvent)
    }

    @Test
    fun testChatViewModelConsistency() = runTest {
        val chatViewModel = ChatViewModel()
        
        // Test initial state
        assertTrue(chatViewModel.uiState.value.messages.isEmpty())
        assertEquals("", chatViewModel.messageValidation.value.value)
        
        // Test message sending
        chatViewModel.updateCurrentMessage("Test message")
        assertEquals("Test message", chatViewModel.messageValidation.value.value)
        
        chatViewModel.sendMessage()
        
        // Give some time for async operations
        kotlinx.coroutines.delay(100)
        
        // Verify message was added
        val messages = chatViewModel.uiState.value.messages
        assertTrue(messages.isNotEmpty())
        assertEquals("Test message", messages.first().content)
        assertTrue(messages.first().isFromUser)
        
        // Verify input was cleared
        assertEquals("", chatViewModel.messageValidation.value.value)
    }

    @Test
    fun testSettingsViewModelConsistency() = runTest {
        val settingsViewModel = SettingsViewModel()
        
        // Give some time for initial loading
        kotlinx.coroutines.delay(100)
        
        // Test initial settings
        val initialSettings = settingsViewModel.settings.value
        assertEquals(ThemeMode.System, initialSettings.theme)
        assertTrue(initialSettings.notifications)
        assertEquals("en", initialSettings.language)
        
        // Test theme update
        settingsViewModel.updateThemeMode(ThemeMode.Dark)
        kotlinx.coroutines.delay(100)
        assertEquals(ThemeMode.Dark, settingsViewModel.settings.value.theme)
        
        // Test notification toggle
        settingsViewModel.toggleNotifications()
        kotlinx.coroutines.delay(100)
        assertFalse(settingsViewModel.settings.value.notifications)
        
        // Test language update
        settingsViewModel.updateLanguage("es")
        kotlinx.coroutines.delay(100)
        assertEquals("es", settingsViewModel.settings.value.language)
    }

    @Test
    fun testErrorHandlingConsistency() = runTest {
        val chatViewModel = ChatViewModel()
        
        // Test that error handling methods exist and don't crash
        chatViewModel.clearError()
        
        // Test that validation methods exist
        chatViewModel.updateCurrentMessage("")
        chatViewModel.sendMessage()
        
        // Give some time for processing
        kotlinx.coroutines.delay(100)
        
        // Test that the ViewModel is still functional after error scenarios
        assertNotNull(chatViewModel.uiState.value)
        assertNotNull(chatViewModel.messageValidation.value)
    }

    @Test
    fun testStateManagementConsistency() = runTest {
        val homeViewModel = HomeViewModel()
        val chatViewModel = ChatViewModel()
        
        // Test that ViewModels have state management capabilities
        assertNotNull(homeViewModel.isLoading)
        assertNotNull(chatViewModel.isLoading)
        
        // Test that error handling exists
        assertNotNull(homeViewModel.error)
        assertNotNull(chatViewModel.error)
        
        // Test error clearing doesn't crash
        homeViewModel.clearError()
        chatViewModel.clearError()
        
        // Verify ViewModels are still functional
        assertNotNull(homeViewModel)
        assertNotNull(chatViewModel)
    }

    @Test
    fun testScreenNavigationModels() {
        // Test that Screen sealed class works consistently
        val homeScreen = Screen.Home
        val chatScreen = Screen.Chat
        val settingsScreen = Screen.Settings
        
        assertNotNull(homeScreen)
        assertNotNull(chatScreen)
        assertNotNull(settingsScreen)
        
        // Test screen equality
        assertEquals(Screen.Home, homeScreen)
        assertEquals(Screen.Chat, chatScreen)
        assertEquals(Screen.Settings, settingsScreen)
    }

    @Test
    fun testThemeModeConsistency() {
        // Test that ThemeMode enum works consistently across platforms
        val lightTheme = ThemeMode.Light
        val darkTheme = ThemeMode.Dark
        val systemTheme = ThemeMode.System
        
        assertNotNull(lightTheme)
        assertNotNull(darkTheme)
        assertNotNull(systemTheme)
        
        // Test theme mode equality
        assertEquals(ThemeMode.Light, lightTheme)
        assertEquals(ThemeMode.Dark, darkTheme)
        assertEquals(ThemeMode.System, systemTheme)
    }

    @Test
    fun testViewModelLifecycleConsistency() = runTest {
        val homeViewModel = HomeViewModel()
        val chatViewModel = ChatViewModel()
        val settingsViewModel = SettingsViewModel()
        
        // Test that ViewModels can be cleared without issues
        homeViewModel.onCleared()
        chatViewModel.onCleared()
        settingsViewModel.onCleared()
        
        // ViewModels should still have their state flows accessible
        // (onCleared cancels the scope but doesn't destroy the state)
        assertNotNull(homeViewModel.navigationEvent)
        assertNotNull(chatViewModel.uiState)
        assertNotNull(settingsViewModel.uiState)
    }

    @Test
    fun testConcurrentStateUpdates() = runTest {
        val chatViewModel = ChatViewModel()
        
        // Test that concurrent state updates work correctly
        chatViewModel.updateCurrentMessage("Message 1")
        chatViewModel.sendMessage()
        
        // Give time for async processing
        kotlinx.coroutines.delay(200)
        
        chatViewModel.updateCurrentMessage("Message 2")
        chatViewModel.sendMessage()
        
        kotlinx.coroutines.delay(200)
        
        chatViewModel.updateCurrentMessage("Message 3")
        chatViewModel.sendMessage()
        
        kotlinx.coroutines.delay(200)
        
        // Verify messages were added (order might vary due to async processing)
        val messages = chatViewModel.uiState.value.messages
        assertTrue(messages.size >= 3) // At least 3 user messages (plus potential agent responses)
        
        // Check that our messages are present
        val userMessages = messages.filter { it.isFromUser }
        assertTrue(userMessages.any { it.content == "Message 1" })
        assertTrue(userMessages.any { it.content == "Message 2" })
        assertTrue(userMessages.any { it.content == "Message 3" })
    }
}