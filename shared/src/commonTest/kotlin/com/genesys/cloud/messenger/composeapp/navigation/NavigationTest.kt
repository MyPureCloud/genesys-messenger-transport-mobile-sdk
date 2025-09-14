package com.genesys.cloud.messenger.composeapp.navigation

import com.genesys.cloud.messenger.composeapp.model.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

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
    
    @Test
    fun testNavigationStateMultipleNavigations() {
        val navigationState = NavigationState()
        
        // Navigate through multiple screens
        navigationState.navigateTo(Screen.Chat)
        assertEquals(Screen.Chat, navigationState.currentScreen)
        
        navigationState.navigateTo(Screen.Settings)
        assertEquals(Screen.Settings, navigationState.currentScreen)
        
        navigationState.navigateTo(Screen.Home)
        assertEquals(Screen.Home, navigationState.currentScreen)
        
        // Navigate back should still go to Home (current implementation)
        navigationState.navigateBack()
        assertEquals(Screen.Home, navigationState.currentScreen)
    }
    
    @Test
    fun testNavigationStateImmutability() {
        val navigationState1 = NavigationState()
        val navigationState2 = NavigationState()
        
        // Both should start at Home
        assertEquals(navigationState1.currentScreen, navigationState2.currentScreen)
        
        // Navigating one should not affect the other
        navigationState1.navigateTo(Screen.Chat)
        assertEquals(Screen.Chat, navigationState1.currentScreen)
        assertEquals(Screen.Home, navigationState2.currentScreen)
    }
    
    @Test
    fun testScreenTypes() {
        val homeScreen = Screen.Home
        val chatScreen = Screen.Chat
        val settingsScreen = Screen.Settings
        
        // Verify they are different instances
        assertNotEquals<Screen>(homeScreen, chatScreen)
        assertNotEquals<Screen>(chatScreen, settingsScreen)
        assertNotEquals<Screen>(homeScreen, settingsScreen)
        
        // Verify they are the correct types
        assertTrue(homeScreen is Screen.Home)
        assertTrue(chatScreen is Screen.Chat)
        assertTrue(settingsScreen is Screen.Settings)
    }
    
    @Test
    fun testScreenEquality() {
        val homeScreen1 = Screen.Home
        val homeScreen2 = Screen.Home
        val chatScreen = Screen.Chat
        
        // Same screen types should be equal
        assertEquals(homeScreen1, homeScreen2)
        
        // Different screen types should not be equal
        assertNotEquals<Screen>(homeScreen1, chatScreen)
    }
    
    @Test
    fun testScreenHashCode() {
        val homeScreen1 = Screen.Home
        val homeScreen2 = Screen.Home
        val chatScreen = Screen.Chat
        
        // Same screen types should have same hash code
        assertEquals(homeScreen1.hashCode(), homeScreen2.hashCode())
        
        // Different screen types should have different hash codes
        assertNotEquals(homeScreen1.hashCode(), chatScreen.hashCode())
    }
    
    @Test
    fun testScreenToString() {
        val homeScreen = Screen.Home
        val chatScreen = Screen.Chat
        val settingsScreen = Screen.Settings
        
        // toString should return meaningful values
        val homeString = homeScreen.toString()
        val chatString = chatScreen.toString()
        val settingsString = settingsScreen.toString()
        
        assertTrue(homeString.isNotEmpty())
        assertTrue(chatString.isNotEmpty())
        assertTrue(settingsString.isNotEmpty())
        
        // Each should be different
        assertNotEquals<String>(homeString, chatString)
        assertNotEquals<String>(chatString, settingsString)
        assertNotEquals<String>(homeString, settingsString)
    }
}