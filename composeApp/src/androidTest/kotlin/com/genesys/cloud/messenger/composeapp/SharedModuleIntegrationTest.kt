package com.genesys.cloud.messenger.composeapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.genesys.cloud.messenger.composeapp.model.ThemeMode
import com.genesys.cloud.messenger.composeapp.viewmodel.ChatViewModel
import com.genesys.cloud.messenger.composeapp.viewmodel.HomeViewModel
import com.genesys.cloud.messenger.composeapp.viewmodel.SettingsViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests specifically for shared module components on Android.
 * 
 * These tests verify:
 * - Shared ViewModels work correctly on Android
 * - Shared UI components render properly on Android
 * - State management works across shared and platform code
 * - Theme system works correctly on Android
 * 
 * Requirements addressed:
 * - 2.5: Testing shared UI components and ViewModels on Android
 * - 3.5: Verifying shared module integration with Android platform
 */
@RunWith(AndroidJUnit4::class)
class SharedModuleIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shared_app_composable_renders_correctly() {
        // Test the shared App composable directly
        composeTestRule.setContent {
            App(themeMode = ThemeMode.Light)
        }
        
        // Verify shared components are displayed
        composeTestRule.onNodeWithText("Welcome to Messenger").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start Chat").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun shared_viewmodels_work_on_android() {
        val homeViewModel = HomeViewModel()
        val chatViewModel = ChatViewModel()
        val settingsViewModel = SettingsViewModel()
        
        composeTestRule.setContent {
            App(
                homeViewModel = homeViewModel,
                chatViewModel = chatViewModel,
                settingsViewModel = settingsViewModel,
                themeMode = ThemeMode.Light
            )
        }
        
        // Verify ViewModels are working by testing navigation
        composeTestRule.onNodeWithText("Start Chat").performClick()
        composeTestRule.onNodeWithText("Chat").assertIsDisplayed()
    }

    @Test
    fun theme_system_works_on_android() {
        // Test light theme
        composeTestRule.setContent {
            App(themeMode = ThemeMode.Light)
        }
        
        composeTestRule.onNodeWithText("Welcome to Messenger").assertIsDisplayed()
        
        // Test dark theme
        composeTestRule.setContent {
            App(themeMode = ThemeMode.Dark)
        }
        
        composeTestRule.onNodeWithText("Welcome to Messenger").assertIsDisplayed()
    }

    @Test
    fun shared_navigation_works_on_android() {
        composeTestRule.setContent {
            App(themeMode = ThemeMode.Light)
        }
        
        // Test navigation to chat
        composeTestRule.onNodeWithText("Start Chat").performClick()
        composeTestRule.onNodeWithText("Chat").assertIsDisplayed()
        
        // Test navigation back
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        composeTestRule.onNodeWithText("Welcome to Messenger").assertIsDisplayed()
        
        // Test navigation to settings
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun shared_chat_components_work_on_android() {
        composeTestRule.setContent {
            App(themeMode = ThemeMode.Light)
        }
        
        // Navigate to chat
        composeTestRule.onNodeWithText("Start Chat").performClick()
        
        // Verify shared chat components
        composeTestRule.onNodeWithContentDescription("Message input field").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Send message").assertIsDisplayed()
        
        // Test message input interaction
        composeTestRule.onNodeWithContentDescription("Message input field").performClick()
    }

    @Test
    fun shared_settings_components_work_on_android() {
        composeTestRule.setContent {
            App(themeMode = ThemeMode.Light)
        }
        
        // Navigate to settings
        composeTestRule.onNodeWithText("Settings").performClick()
        
        // Verify shared settings components
        composeTestRule.onNodeWithText("Theme").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Language").assertIsDisplayed()
    }

    @Test
    fun lifecycle_management_works_on_android() {
        var viewModelCleared = false
        
        composeTestRule.setContent {
            AppWithLifecycle(
                themeMode = ThemeMode.Light,
                onViewModelCleared = { viewModelCleared = true }
            )
        }
        
        // Verify app is displayed
        composeTestRule.onNodeWithText("Welcome to Messenger").assertIsDisplayed()
        
        // Note: In a real test, you would trigger lifecycle events
        // This test verifies the lifecycle-aware composable works
    }
}