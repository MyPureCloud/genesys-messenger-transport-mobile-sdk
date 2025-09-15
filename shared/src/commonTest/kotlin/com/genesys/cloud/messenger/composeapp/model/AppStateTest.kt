package com.genesys.cloud.messenger.composeapp.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppStateTest {
    
    @Test
    fun testAppStateDefaults() {
        val appState = AppState()
        
        assertFalse(appState.isLoading)
        assertEquals(Screen.Home, appState.currentScreen)
        assertNull(appState.error)
    }
    
    @Test
    fun testAppStateWithCustomValues() {
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
    fun testAppStateCopy() {
        val originalState = AppState(
            isLoading = false,
            currentScreen = Screen.Home,
            error = null
        )
        
        val modifiedState = originalState.copy(
            isLoading = true,
            currentScreen = Screen.Settings
        )
        
        // Original should be unchanged
        assertFalse(originalState.isLoading)
        assertEquals(Screen.Home, originalState.currentScreen)
        
        // Modified should have new values
        assertTrue(modifiedState.isLoading)
        assertEquals(Screen.Settings, modifiedState.currentScreen)
        
        // Unchanged properties should remain the same
        assertNull(modifiedState.error)
    }
    
    @Test
    fun testAppStateScreenTransitions() {
        val appState = AppState()
        
        val interactionState = appState.copy(currentScreen = Screen.Interaction)
        val settingsState = interactionState.copy(currentScreen = Screen.Settings)
        val homeState = settingsState.copy(currentScreen = Screen.Home)
        
        assertEquals(Screen.Home, appState.currentScreen)
        assertEquals(Screen.Interaction, interactionState.currentScreen)
        assertEquals(Screen.Settings, settingsState.currentScreen)
        assertEquals(Screen.Home, homeState.currentScreen)
    }
    
    @Test
    fun testAppStateLoadingStates() {
        val appState = AppState()
        
        val loadingState = appState.copy(isLoading = true)
        val loadedState = loadingState.copy(isLoading = false)
        
        assertFalse(appState.isLoading)
        assertTrue(loadingState.isLoading)
        assertFalse(loadedState.isLoading)
    }
    
    @Test
    fun testAppStateErrorHandling() {
        val appState = AppState()
        
        val errorState = appState.copy(error = "Something went wrong")
        val clearedErrorState = errorState.copy(error = null)
        
        assertNull(appState.error)
        assertEquals("Something went wrong", errorState.error)
        assertNull(clearedErrorState.error)
    }
    
    @Test
    fun testAppStateEquality() {
        val state1 = AppState(
            isLoading = true,
            currentScreen = Screen.Interaction,
            error = "Error"
        )
        
        val state2 = AppState(
            isLoading = true,
            currentScreen = Screen.Interaction,
            error = "Error"
        )
        
        val state3 = AppState(
            isLoading = false,
            currentScreen = Screen.Interaction,
            error = "Error"
        )
        
        assertEquals(state1, state2)
        assertNotEquals(state1, state3)
    }
    
    @Test
    fun testAppStateHashCode() {
        val state1 = AppState(
            isLoading = true,
            currentScreen = Screen.Interaction,
            error = "Error"
        )
        
        val state2 = AppState(
            isLoading = true,
            currentScreen = Screen.Interaction,
            error = "Error"
        )
        
        assertEquals(state1.hashCode(), state2.hashCode())
    }
    
    @Test
    fun testAppStateToString() {
        val appState = AppState(
            isLoading = true,
            currentScreen = Screen.Interaction,
            error = "Test error"
        )
        
        val toString = appState.toString()
        assertTrue(toString.contains("isLoading"))
        assertTrue(toString.contains("currentScreen"))
        assertTrue(toString.contains("error"))
    }
}