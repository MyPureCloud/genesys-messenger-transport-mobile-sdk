package com.genesys.cloud.messenger.composeapp.navigation

import com.genesys.cloud.messenger.composeapp.model.Screen
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for navigation functionality
 */
class NavigationTest {
    
    @Test
    fun testScreenToRoute() {
        assertEquals("home", Screen.Home.toRoute())
        assertEquals("chat", Screen.Chat.toRoute())
        assertEquals("settings", Screen.Settings.toRoute())
    }
    
    @Test
    fun testRouteToScreen() {
        assertEquals(Screen.Home, "home".toScreen())
        assertEquals(Screen.Chat, "chat".toScreen())
        assertEquals(Screen.Settings, "settings".toScreen())
    }
    
    @Test
    fun testRouteToScreenWithInvalidRoute() {
        // Should default to Home for invalid routes
        assertEquals(Screen.Home, "invalid".toScreen())
        assertEquals(Screen.Home, "".toScreen())
    }
    
    @Test
    fun testNavigationState() {
        val navigationState = NavigationState()
        
        // Initial state should be Home
        assertEquals(Screen.Home, navigationState.currentScreen)
        
        // Navigate to Chat
        navigationState.navigateTo(Screen.Chat)
        assertEquals(Screen.Chat, navigationState.currentScreen)
        
        // Navigate to Settings
        navigationState.navigateTo(Screen.Settings)
        assertEquals(Screen.Settings, navigationState.currentScreen)
        
        // Navigate back (should go to Home)
        navigationState.navigateBack()
        assertEquals(Screen.Home, navigationState.currentScreen)
    }
}