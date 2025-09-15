package com.genesys.cloud.messenger.composeapp.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DataModelsTest {

    @Test
    fun testAppStateDefaults() {
        val appState = AppState()
        
        assertFalse(appState.isLoading)
        assertEquals(Screen.Home, appState.currentScreen)
        assertEquals(null, appState.error)
    }

    @Test
    fun testAppStateWithData() {
        val appState = AppState(
            isLoading = true,
            currentScreen = Screen.Interaction,
            error = "Test error"
        )
        
        assertTrue(appState.isLoading)
        assertEquals(Screen.Interaction, appState.currentScreen)
        assertEquals("Test error", appState.error)
    }

    @Test
    fun testAppSettingsDefaults() {
        val settings = AppSettings()
        
        assertEquals("", settings.deploymentId)
        assertEquals("", settings.region)
    }

    @Test
    fun testAppSettingsCustom() {
        val settings = AppSettings(
            deploymentId = "12345678-1234-1234-1234-123456789abc",
            region = "mypurecloud.com"
        )
        
        assertEquals("12345678-1234-1234-1234-123456789abc", settings.deploymentId)
        assertEquals("mypurecloud.com", settings.region)
    }

    @Test
    fun testScreenTypes() {
        val homeScreen = Screen.Home
        val interactionScreen = Screen.Interaction
        val settingsScreen = Screen.Settings
        
        // Verify they are the correct types
        assertTrue(homeScreen is Screen.Home)
        assertTrue(interactionScreen is Screen.Interaction)
        assertTrue(settingsScreen is Screen.Settings)
        
        // Verify they are different by checking their class names
        assertFalse(homeScreen::class.equals(interactionScreen::class))
        assertFalse(interactionScreen::class.equals(settingsScreen::class))
        assertFalse(homeScreen::class.equals(settingsScreen::class))
    }

    @Test
    fun testAppConfigValues() {
        // Test that AppConfig contains expected default values
        assertEquals("00c966c5-8f88-42b5-ae9b-fa81b5721569", com.genesys.cloud.messenger.composeapp.config.AppConfig.DEFAULT_DEPLOYMENT_ID)
        assertEquals("inindca.com", com.genesys.cloud.messenger.composeapp.config.AppConfig.DEFAULT_REGION)
        assertTrue(com.genesys.cloud.messenger.composeapp.config.AppConfig.AVAILABLE_REGIONS.isNotEmpty())
        assertTrue(com.genesys.cloud.messenger.composeapp.config.AppConfig.AVAILABLE_REGIONS.contains("inindca.com"))
    }
}