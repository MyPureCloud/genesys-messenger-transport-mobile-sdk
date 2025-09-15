package com.genesys.cloud.messenger.composeapp.viewmodel

import com.genesys.cloud.messenger.composeapp.model.MessagingClientState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for socket message processing functionality in TestBedViewModel
 */
class TestBedViewModelSocketMessageTest {
    
    @Test
    fun testSocketMessages_initiallyEmpty() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // Then
        val socketMessages = viewModel.socketMessages.value
        assertTrue(socketMessages.isEmpty(), "Socket messages should be initially empty")
    }
    
    @Test
    fun testClientStateProperty_initialValue() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // Then
        assertEquals(MessagingClientState.Idle, viewModel.clientState, "Initial client state should be Idle")
    }
    
    @Test
    fun testSocketMessageProperty_initialValue() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // Then
        assertEquals("", viewModel.socketMessage, "Initial socket message should be empty")
    }
    
    @Test
    fun testCommandExecution_createsSocketMessages() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.command = "deployment"
        
        // When
        viewModel.onCommandSend()
        
        // Then
        val socketMessages = viewModel.socketMessages.value
        assertTrue(socketMessages.isNotEmpty(), "Socket messages should not be empty after command execution")
        
        // Should have at least command execution start message and result message
        assertTrue(socketMessages.size >= 2, "Should have at least 2 socket messages")
        
        // Check that we have a command execution message
        val commandMessage = socketMessages.find { it.type == "Command" }
        assertTrue(commandMessage != null, "Should have a command execution message")
        assertTrue(commandMessage.content.contains("deployment"), "Command message should contain command name")
        
        // Check that we have a result message
        val resultMessage = socketMessages.find { it.type == "Command Result" }
        assertTrue(resultMessage != null, "Should have a command result message")
    }
    
    @Test
    fun testInvalidCommand_createsErrorMessage() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.command = "invalidcommand"
        
        // When
        viewModel.onCommandSend()
        
        // Then
        val socketMessages = viewModel.socketMessages.value
        assertTrue(socketMessages.isNotEmpty(), "Socket messages should not be empty")
        
        // Should have an error message for unknown command
        val errorMessage = socketMessages.find { it.type == "Error" }
        assertTrue(errorMessage != null, "Should have an error message")
        assertTrue(errorMessage.content.contains("Unknown command"), "Error message should indicate unknown command")
    }
    
    @Test
    fun testEmptyCommand_createsErrorMessage() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.command = ""
        
        // When
        viewModel.onCommandSend()
        
        // Then
        val socketMessages = viewModel.socketMessages.value
        assertTrue(socketMessages.isNotEmpty(), "Socket messages should not be empty")
        
        // Should have an error message for empty command
        val errorMessage = socketMessages.find { it.type == "Error" }
        assertTrue(errorMessage != null, "Should have an error message")
        assertTrue(errorMessage.content.contains("cannot be empty"), "Error message should indicate empty command")
    }
    
    @Test
    fun testCommandWithRequiredInput_validatesInput() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.command = "send" // This command requires input
        
        // When
        viewModel.onCommandSend()
        
        // Then
        val socketMessages = viewModel.socketMessages.value
        assertTrue(socketMessages.isNotEmpty(), "Socket messages should not be empty")
        
        // Should have an error message for missing required input
        val errorMessage = socketMessages.find { it.type == "Error" }
        assertTrue(errorMessage != null, "Should have an error message")
        assertTrue(errorMessage.content.contains("requires input"), "Error message should indicate missing input")
    }
    
    @Test
    fun testSocketMessageLimit_enforcesMaxMessages() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When - Execute many commands to exceed the limit
        repeat(600) { // More than MAX_SOCKET_MESSAGES (500)
            viewModel.command = "deployment"
            viewModel.onCommandSend()
        }
        
        // Then
        val socketMessages = viewModel.socketMessages.value
        assertTrue(socketMessages.size <= 500, "Socket messages should not exceed maximum limit")
    }
}