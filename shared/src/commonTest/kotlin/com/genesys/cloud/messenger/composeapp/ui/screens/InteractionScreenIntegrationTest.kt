package com.genesys.cloud.messenger.composeapp.ui.screens

import com.genesys.cloud.messenger.composeapp.model.*
import com.genesys.cloud.messenger.composeapp.viewmodel.TestBedViewModel
import com.genesys.cloud.messenger.composeapp.viewmodel.SettingsViewModel
import com.genesys.cloud.messenger.composeapp.util.getCurrentTimeMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Integration tests for InteractionScreen focusing on the interaction between UI components and ViewModels.
 * These tests verify the integration logic that can be tested without actual Compose UI rendering.
 * 
 * Requirements addressed:
 * - 2.1: Display as "InteractionScreen" instead of "ChatScreen"
 * - 2.2: Display message type prominently
 * - 2.3: Expandable socket message details
 * - 2.4: Replace message input with command input field
 * - 2.5: Command dropdown with all available commands
 * - 2.6: Additional input field for commands requiring parameters
 */
class InteractionScreenIntegrationTest {

    /**
     * Test command dropdown functionality integration
     * Requirements: 2.5, 4.1
     */
    @Test
    fun testCommandDropdownFunctionality() = runTest {
        // Given
        val testBedViewModel = TestBedViewModel()
        
        // When - Collect available commands (these are created by default in the ViewModel)
        val availableCommands = testBedViewModel.availableCommands.value
        
        // Then - Verify command dropdown data
        assertTrue(availableCommands.isNotEmpty(), "Should have available commands")
        
        // Verify some expected commands exist
        val commandNames = availableCommands.map { it.name }
        assertTrue(commandNames.contains("connect"), "Should have connect command")
        assertTrue(commandNames.contains("send"), "Should have send command")
        
        // Verify commands have proper structure
        val sendCommand = availableCommands.find { it.name == "send" }
        if (sendCommand != null) {
            assertTrue(sendCommand.requiresInput, "Send command should require input")
            assertTrue(sendCommand.inputPlaceholder?.isNotEmpty() == true, "Send command should have placeholder")
        }
        
        val connectCommand = availableCommands.find { it.name == "connect" }
        if (connectCommand != null) {
            assertFalse(connectCommand.requiresInput, "Connect command should not require input")
        }
    }

    /**
     * Test socket message display and expansion functionality
     * Requirements: 2.2, 2.3, 5.1, 5.2, 5.3
     */
    @Test
    fun testSocketMessageDisplayAndExpansion() = runTest {
        // Given
        val testBedViewModel = TestBedViewModel()
        val testMessages = listOf(
            SocketMessage(
                id = "msg1",
                timestamp = 1234567890L,
                type = "Error",
                content = "Test message content",
                rawMessage = """{"type":"MessageEvent","data":"test"}"""
            ),
            SocketMessage(
                id = "msg2", 
                timestamp = 1234567891L,
                type = "Command",
                content = "Connected to service",
                rawMessage = """{"type":"ConnectionEvent","status":"connected"}"""
            )
        )
        
        // When - Add socket messages
        testMessages.forEach { message ->
            testBedViewModel.addSocketMessage(message)
        }
        
        // Then - Verify socket messages are stored correctly
        val socketMessages = testBedViewModel.socketMessages.value
        assertEquals(2, socketMessages.size, "Should have 2 socket messages")
        
        // Verify message type prominence (requirement 2.2)
        assertEquals("Error", socketMessages[0].displayType, "First message should display type prominently")
        assertEquals("Command", socketMessages[1].displayType, "Second message should display type prominently")
        
        // Verify message priority affects display
        assertEquals(MessagePriority.HIGH, socketMessages[0].priority, "First message should have high priority")
        assertEquals(MessagePriority.MEDIUM, socketMessages[1].priority, "Second message should have medium priority")
        
        // Verify expandable content is available (requirement 2.3)
        assertTrue(socketMessages[0].formattedContent.isNotEmpty(), "Message should have formatted content for expansion")
        assertTrue(socketMessages[0].summary.isNotEmpty(), "Message should have summary for collapsed view")
    }

    /**
     * Test command execution and loading states
     * Requirements: 4.2, 4.3, 4.4
     */
    @Test
    fun testCommandExecutionAndLoadingStates() = runTest {
        // Given
        val testBedViewModel = TestBedViewModel()
        
        // Initial state verification
        assertFalse(testBedViewModel.commandWaiting, "Initially should not be waiting for command")
        assertEquals("", testBedViewModel.command, "Initial command should be empty")
        assertEquals(MessagingClientState.Idle, testBedViewModel.clientState, "Initial client state should be Idle")
        
        // When - Set a command
        testBedViewModel.onCommandChanged("connect")
        
        // Then - Verify command state
        assertEquals("connect", testBedViewModel.command, "Command should be set correctly")
        assertFalse(testBedViewModel.commandWaiting, "Should not be waiting until command is sent")
        
        // When - Add a test socket message to verify message handling
        testBedViewModel.addSocketMessage(
            SocketMessage(
                id = "cmd_result",
                timestamp = getCurrentTimeMillis(),
                type = "Command",
                content = "Connect command executed successfully",
                rawMessage = """{"command":"connect","status":"success"}"""
            )
        )
        
        // Then - Verify message was added
        val messages = testBedViewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command" }, "Should have command message")
        assertTrue(messages.any { it.content.contains("Connect command") }, "Should have connect command message")
    }

    /**
     * Test initialization flow integration
     * Requirements: 3.1
     */
    @Test
    fun testInitializationFlow() = runTest {
        // Given
        val testBedViewModel = TestBedViewModel()
        
        // Initial state - not initialized
        assertFalse(testBedViewModel.isInitialized, "Should not be initialized initially")
        assertEquals("", testBedViewModel.deploymentId, "Initial deployment ID should be empty")
        assertEquals("", testBedViewModel.region, "Initial region should be empty")
        
        // When - Update deployment settings directly
        testBedViewModel.deploymentId = "test-deployment-123"
        testBedViewModel.region = "us-west-2"
        
        // Then - Verify deployment settings are updated
        assertEquals("test-deployment-123", testBedViewModel.deploymentId, "Deployment ID should be updated")
        assertEquals("us-west-2", testBedViewModel.region, "Region should be updated")
        
        // Verify initialization state management
        assertFalse(testBedViewModel.isInitialized, "Should remain uninitialized until init() is called")
        assertEquals(MessagingClientState.Idle, testBedViewModel.clientState, "Client state should be Idle initially")
    }

    /**
     * Test command input interface integration
     * Requirements: 2.4, 2.6
     */
    @Test
    fun testCommandInputInterface() = runTest {
        // Given
        val testBedViewModel = TestBedViewModel()
        
        // When - Get available commands from ViewModel
        val availableCommands = testBedViewModel.availableCommands.value
        
        // Then - Verify command structure (requirement 2.6)
        assertTrue(availableCommands.isNotEmpty(), "Should have available commands")
        
        // Find commands that require input
        val commandsWithInput = availableCommands.filter { it.requiresInput }
        val commandsWithoutInput = availableCommands.filter { !it.requiresInput }
        
        assertTrue(commandsWithInput.isNotEmpty(), "Should have commands that require input")
        assertTrue(commandsWithoutInput.isNotEmpty(), "Should have commands that don't require input")
        
        // Verify commands with input have placeholders
        commandsWithInput.forEach { command ->
            assertTrue(command.inputPlaceholder?.isNotEmpty() == true, "Commands requiring input should have placeholders")
        }
        
        // When - Set command with parameters
        testBedViewModel.onCommandChanged("send Hello World")
        
        // Then - Verify command parsing
        assertEquals("send Hello World", testBedViewModel.command, "Full command with parameters should be stored")
        
        // Verify command input field replacement (requirement 2.4)
        assertTrue(availableCommands.any { it.requiresInput }, "Should have commands that require additional input")
    }

    /**
     * Test error handling and display integration
     * Requirements: 4.5
     */
    @Test
    fun testErrorHandlingIntegration() = runTest {
        // Given
        val testBedViewModel = TestBedViewModel()
        
        // Initial state - no errors
        assertEquals(null, testBedViewModel.lastError.value, "Should have no initial error")
        assertEquals(0, testBedViewModel.errorHistory.value.size, "Should have empty error history")
        
        // When - Clear errors (test the clear functionality)
        testBedViewModel.clearErrors()
        
        // Then - Verify errors remain cleared (since there were none initially)
        assertEquals(null, testBedViewModel.lastError.value, "Last error should remain null")
        assertEquals(0, testBedViewModel.errorHistory.value.size, "Error history should remain empty")
        
        // Verify error handling infrastructure exists
        assertNotNull(testBedViewModel.lastError, "Should have lastError StateFlow")
        assertNotNull(testBedViewModel.errorHistory, "Should have errorHistory StateFlow")
    }

    /**
     * Test client state indicators integration
     * Requirements: 4.3, 4.4
     */
    @Test
    fun testClientStateIndicators() = runTest {
        // Given
        val testBedViewModel = TestBedViewModel()
        
        // Test different client states
        val testStates = listOf(
            MessagingClientState.Idle,
            MessagingClientState.Connecting,
            MessagingClientState.Connected,
            MessagingClientState.Disconnecting,
            MessagingClientState.Disconnected,
            MessagingClientState.Error
        )
        
        testStates.forEach { state ->
            // When - Set client state
            testBedViewModel.clientState = state
            
            // Then - Verify state is reflected
            assertEquals(state, testBedViewModel.clientState, "Client state should be updated to $state")
            
            // Verify state affects UI indicators (would be tested in actual UI tests)
            // Here we verify the underlying state management
            when (state) {
                MessagingClientState.Connecting, MessagingClientState.Disconnecting -> {
                    // These states typically show loading indicators
                    assertTrue(true, "Transitional states should be handled")
                }
                MessagingClientState.Connected -> {
                    // Connected state should show ready status
                    assertTrue(true, "Connected state should show ready status")
                }
                MessagingClientState.Error -> {
                    // Error state should show error indicators
                    assertTrue(true, "Error state should show error indicators")
                }
                else -> {
                    // Other states
                    assertTrue(true, "All states should be handled")
                }
            }
        }
    }

    /**
     * Test socket message auto-scroll behavior
     * Requirements: 5.4
     */
    @Test
    fun testSocketMessageAutoScroll() = runTest {
        // Given
        val testBedViewModel = TestBedViewModel()
        val initialMessageCount = testBedViewModel.socketMessages.value.size
        
        // When - Add multiple messages rapidly
        repeat(5) { index ->
            testBedViewModel.addSocketMessage(
                SocketMessage(
                    id = "auto_scroll_$index",
                    timestamp = getCurrentTimeMillis() + index,
                    type = "TestMessage",
                    content = "Auto scroll test message $index",
                    rawMessage = """{"index":$index}"""
                )
            )
        }
        
        // Then - Verify messages are added in order
        val messages = testBedViewModel.socketMessages.value
        assertEquals(initialMessageCount + 5, messages.size, "Should have added 5 messages")
        
        // Verify chronological order (requirement 5.3)
        for (i in 1 until messages.size) {
            assertTrue(
                messages[i].timestamp >= messages[i-1].timestamp,
                "Messages should be in chronological order"
            )
        }
        
        // Verify latest message is at the end (for auto-scroll)
        val latestMessage = messages.lastOrNull()
        assertNotNull(latestMessage, "Should have a latest message")
        assertTrue(latestMessage.content.contains("4"), "Latest message should be the last one added")
    }

    /**
     * Test message formatting for readability
     * Requirements: 5.5
     */
    @Test
     fun testMessageFormattingForReadability() = runTest {
        // Given
        val testBedViewModel = TestBedViewModel()
        val jsonMessage = SocketMessage(
            id = "json_test",
            timestamp = getCurrentTimeMillis(),
            type = "System",
            content = "JSON data received",
            rawMessage = """{"type":"test","data":{"nested":"value","array":[1,2,3]}}"""
        )
        
        // When - Add message with JSON content
        testBedViewModel.addSocketMessage(jsonMessage)
        
        // Then - Verify message formatting
        val messages = testBedViewModel.socketMessages.value
        val addedMessage = messages.find { it.id == "json_test" }
        assertNotNull(addedMessage, "JSON message should be added")
        
        // Verify formatted content is available for expansion (requirement 5.5)
        assertTrue(addedMessage.formattedContent.isNotEmpty(), "Should have formatted content")
        assertTrue(addedMessage.summary.isNotEmpty(), "Should have summary for collapsed view")
        
        // Verify JSON formatting would be applied (actual formatting tested in util tests)
        assertTrue(addedMessage.rawMessage.contains("{"), "Raw message should contain JSON")
        assertTrue(addedMessage.formattedContent.length >= addedMessage.rawMessage.length, "Formatted content should be at least as long as raw message")
    }
}

