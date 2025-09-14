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
        assertTrue(appState.messages.isEmpty())
        assertEquals(Screen.Home, appState.currentScreen)
        assertNull(appState.error)
    }
    
    @Test
    fun testAppStateWithCustomValues() {
        val messages = listOf(
            ChatMessage("1", "Hello", 1000L, true),
            ChatMessage("2", "Hi there", 2000L, false)
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
    fun testAppStateCopy() {
        val originalState = AppState(
            isLoading = false,
            messages = emptyList(),
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
        assertTrue(modifiedState.messages.isEmpty())
        assertNull(modifiedState.error)
    }
    
    @Test
    fun testAppStateWithMessages() {
        val message1 = ChatMessage("1", "First message", 1000L, true)
        val message2 = ChatMessage("2", "Second message", 2000L, false)
        val message3 = ChatMessage("3", "Third message", 3000L, true)
        
        val appState = AppState(messages = listOf(message1, message2, message3))
        
        assertEquals(3, appState.messages.size)
        assertEquals(message1, appState.messages[0])
        assertEquals(message2, appState.messages[1])
        assertEquals(message3, appState.messages[2])
    }
    
    @Test
    fun testAppStateAddMessage() {
        val initialMessage = ChatMessage("1", "Initial", 1000L, true)
        val appState = AppState(messages = listOf(initialMessage))
        
        val newMessage = ChatMessage("2", "New message", 2000L, false)
        val updatedState = appState.copy(
            messages = appState.messages + newMessage
        )
        
        assertEquals(1, appState.messages.size)
        assertEquals(2, updatedState.messages.size)
        assertEquals(newMessage, updatedState.messages.last())
    }
    
    @Test
    fun testAppStateRemoveMessages() {
        val messages = listOf(
            ChatMessage("1", "First", 1000L, true),
            ChatMessage("2", "Second", 2000L, false),
            ChatMessage("3", "Third", 3000L, true)
        )
        
        val appState = AppState(messages = messages)
        val clearedState = appState.copy(messages = emptyList())
        
        assertEquals(3, appState.messages.size)
        assertTrue(clearedState.messages.isEmpty())
    }
    
    @Test
    fun testAppStateScreenTransitions() {
        val appState = AppState()
        
        val chatState = appState.copy(currentScreen = Screen.Chat)
        val settingsState = chatState.copy(currentScreen = Screen.Settings)
        val homeState = settingsState.copy(currentScreen = Screen.Home)
        
        assertEquals(Screen.Home, appState.currentScreen)
        assertEquals(Screen.Chat, chatState.currentScreen)
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
        val messages = listOf(ChatMessage("1", "Test", 1000L, true))
        
        val state1 = AppState(
            isLoading = true,
            messages = messages,
            currentScreen = Screen.Chat,
            error = "Error"
        )
        
        val state2 = AppState(
            isLoading = true,
            messages = messages,
            currentScreen = Screen.Chat,
            error = "Error"
        )
        
        val state3 = AppState(
            isLoading = false,
            messages = messages,
            currentScreen = Screen.Chat,
            error = "Error"
        )
        
        assertEquals(state1, state2)
        assertNotEquals(state1, state3)
    }
    
    @Test
    fun testAppStateHashCode() {
        val messages = listOf(ChatMessage("1", "Test", 1000L, true))
        
        val state1 = AppState(
            isLoading = true,
            messages = messages,
            currentScreen = Screen.Chat,
            error = "Error"
        )
        
        val state2 = AppState(
            isLoading = true,
            messages = messages,
            currentScreen = Screen.Chat,
            error = "Error"
        )
        
        assertEquals(state1.hashCode(), state2.hashCode())
    }
    
    @Test
    fun testAppStateToString() {
        val appState = AppState(
            isLoading = true,
            messages = listOf(ChatMessage("1", "Test", 1000L, true)),
            currentScreen = Screen.Chat,
            error = "Test error"
        )
        
        val toString = appState.toString()
        assertTrue(toString.contains("isLoading"))
        assertTrue(toString.contains("messages"))
        assertTrue(toString.contains("currentScreen"))
        assertTrue(toString.contains("error"))
    }
    
    @Test
    fun testAppStateImmutability() {
        val originalMessages = listOf(ChatMessage("1", "Original", 1000L, true))
        val appState = AppState(messages = originalMessages)
        
        // Modifying the original list should not affect the app state
        val mutableMessages = originalMessages.toMutableList()
        mutableMessages.add(ChatMessage("2", "Added", 2000L, false))
        
        assertEquals(1, appState.messages.size)
        assertEquals("Original", appState.messages[0].content)
    }
}