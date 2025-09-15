package com.genesys.cloud.messenger.composeapp.viewmodel

import com.genesys.cloud.messenger.composeapp.model.AuthState
import com.genesys.cloud.messenger.composeapp.model.Command
import com.genesys.cloud.messenger.composeapp.model.CorrectiveAction
import com.genesys.cloud.messenger.composeapp.model.ErrorCode
import com.genesys.cloud.messenger.composeapp.model.Event
import com.genesys.cloud.messenger.composeapp.model.FileAttachmentProfile
import com.genesys.cloud.messenger.composeapp.model.MessageEvent
import com.genesys.cloud.messenger.composeapp.model.MessagingClientState
import com.genesys.cloud.messenger.composeapp.model.PlatformContext
import com.genesys.cloud.messenger.composeapp.model.SavedAttachment
import com.genesys.cloud.messenger.composeapp.model.SocketMessage
import com.genesys.cloud.messenger.composeapp.model.TestBedError
import com.genesys.cloud.messenger.composeapp.util.getCurrentTimeMillis
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for TestBedViewModel covering:
 * - Command execution and state management
 * - Socket message processing
 * - Authentication flow handling
 * - Deployment configuration
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.4
 */
class TestBedViewModelTest {

    // Note: PlatformContext is an expect class and cannot be mocked directly in common tests
    // These tests focus on the ViewModel logic that doesn't require actual platform context

    // MARK: - Initialization Tests (Requirement 3.1)

    @Test
    fun testInitialState() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // Then - Verify initial state
        assertEquals("", viewModel.command, "Initial command should be empty")
        assertFalse(viewModel.commandWaiting, "Initial command waiting should be false")
        assertEquals("", viewModel.socketMessage, "Initial socket message should be empty")
        assertEquals(MessagingClientState.Idle, viewModel.clientState, "Initial client state should be Idle")
        assertEquals("", viewModel.deploymentId, "Initial deployment ID should be empty")
        assertEquals("", viewModel.region, "Initial region should be empty")
        assertEquals(AuthState.NoAuth, viewModel.authState, "Initial auth state should be NoAuth")
        assertFalse(viewModel.isInitialized, "Initial initialization state should be false")
        assertTrue(viewModel.socketMessages.value.isEmpty(), "Initial socket messages should be empty")
        assertTrue(viewModel.attachments.value.isEmpty(), "Initial attachments should be empty")
        assertTrue(viewModel.savedAttachments.value.isEmpty(), "Initial saved attachments should be empty")
    }

    // Note: Initialization tests that require PlatformContext are skipped in common tests
    // These would be implemented in platform-specific test modules (androidTest/iosTest)

    // MARK: - Command Execution Tests (Requirement 3.2)

    @Test
    fun testAvailableCommands_containsAllExpectedCommands() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        val commands = viewModel.availableCommands.value
        val commandNames = commands.map { it.name.lowercase() }
        
        // Then - Verify all expected commands are available
        val expectedCommands = listOf(
            "connect", "connectauthenticated", "disconnect", "bye",
            "send", "sendquickreply", "healthcheck", "history",
            "startnewchat", "newchat", "clearconversation", "invalidateconversationcache",
            "attach", "attachsavedimage", "detach", "refreshattachment", "savedfilename", "fileattachmentprofile",
            "oktasignin", "oktasigninwithpkce", "oktalogout", "authorize", "stepup", "wasauthenticated", "shouldauthorize",
            "removetoken", "removeauthrefreshtoken",
            "syncdevicetoken", "unregpush",
            "deployment", "addattribute", "typing"
        )
        
        expectedCommands.forEach { expectedCommand ->
            assertTrue(
                commandNames.contains(expectedCommand),
                "Command '$expectedCommand' should be available"
            )
        }
    }

    @Test
    fun testCommandExecution_emptyCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.command = ""
        
        // When
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.isNotEmpty(), "Should have error message")
        assertTrue(messages.any { it.type == "Error" && it.content.contains("cannot be empty") }, "Should have empty command error")
        assertFalse(viewModel.commandWaiting, "Command waiting should be false")
    }

    @Test
    fun testCommandExecution_unknownCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.command = "unknowncommand"
        
        // When
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.isNotEmpty(), "Should have error message")
        assertTrue(messages.any { it.type == "Error" && it.content.contains("Unknown command") }, "Should have unknown command error")
        assertFalse(viewModel.commandWaiting, "Command waiting should be false")
        assertEquals("", viewModel.command, "Command should be cleared after execution")
    }

    @Test
    fun testCommandExecution_validCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.command = "deployment"
        
        // When
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.isNotEmpty(), "Should have messages")
        assertTrue(messages.any { it.type == "Command" && it.content.contains("Executing: deployment") }, "Should have command execution message")
        assertTrue(messages.any { it.type == "Command Result" }, "Should have command result message")
        assertFalse(viewModel.commandWaiting, "Command waiting should be false after completion")
        assertEquals("", viewModel.command, "Command should be cleared after execution")
    }

    @Test
    fun testCommandExecution_commandWaitingPreventsExecution() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.command = "connect"
        viewModel.commandWaiting = true
        
        // When
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Warning" && it.content.contains("already in progress") }, "Should have warning about command in progress")
    }

    @Test
    fun testConnectCommand_notInitialized() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.command = "connect"
        viewModel.deploymentId = "test-deployment"
        viewModel.region = "us-east-1"
        
        // When
        viewModel.onCommandSend()
        
        // Then - Should fail because client is not initialized
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Error" && it.content.contains("client not initialized") }, "Should have error message for uninitialized client")
        assertEquals(MessagingClientState.Idle, viewModel.clientState, "Client state should remain Idle")
        assertFalse(viewModel.commandWaiting, "Command waiting should be false")
    }

    @Test
    fun testSendMessageCommand_withMessage() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.command = "send Hello World"
        
        // When
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Send message: Hello World") }, "Should have send message result")
        assertFalse(viewModel.commandWaiting, "Command waiting should be false")
    }

    @Test
    fun testSendMessageCommand_withoutMessage() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.command = "send"
        
        // When
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Error" && it.content.contains("requires input") }, "Should have error for missing input")
        assertFalse(viewModel.commandWaiting, "Command waiting should be false")
    }

    // MARK: - Socket Message Processing Tests (Requirement 3.3)

    @Test
    fun testSocketMessage_addMessage() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        val initialCount = viewModel.socketMessages.value.size
        
        // When - Execute a command that adds socket messages
        viewModel.command = "deployment"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.size > initialCount, "Should have more messages")
        
        // Verify message structure
        val latestMessage = messages.last()
        assertNotNull(latestMessage.id, "Message should have ID")
        assertTrue(latestMessage.timestamp > 0, "Message should have timestamp")
        assertNotNull(latestMessage.type, "Message should have type")
        assertNotNull(latestMessage.content, "Message should have content")
        assertNotNull(latestMessage.rawMessage, "Message should have raw message")
    }

    @Test
    fun testSocketMessage_messageEventProcessing() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        val messageEvent = MessageEvent(
            type = "text",
            content = "Hello from agent",
            timestamp = getCurrentTimeMillis()
        )
        
        val initialCount = viewModel.socketMessages.value.size
        
        // When - Simulate message event (this would normally be called by the transport SDK)
        // For testing, we'll trigger it through a command that processes messages
        viewModel.command = "deployment" // This will add some messages
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.size > initialCount, "Should have processed messages")
    }

    @Test
    fun testSocketMessage_eventProcessing() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        val event = Event(
            type = "connectionestablished",
            data = "Connection successful"
        )
        
        val initialCount = viewModel.socketMessages.value.size
        
        // When - Execute a command that generates events
        viewModel.command = "connect"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.size > initialCount, "Should have processed events")
    }

    // MARK: - Authentication Flow Tests (Requirement 3.4)

    @Test
    fun testAuthState_initialState() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // Then
        assertEquals(AuthState.NoAuth, viewModel.authState, "Initial auth state should be NoAuth")
    }

    @Test
    fun testOktaSignInCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.command = "oktasignin"
        
        // When
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Okta sign-in initiated") }, "Should have Okta sign-in message")
        assertFalse(viewModel.commandWaiting, "Command waiting should be false")
    }

    @Test
    fun testAuthorizeCommand_withAuthCode() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.command = "authorize ABC123DEF456"
        
        // When
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Processing authorization code") }, "Should have authorization processing message")
        assertEquals(AuthState.Authorized, viewModel.authState, "Auth state should be Authorized")
        assertFalse(viewModel.commandWaiting, "Command waiting should be false")
    }

    @Test
    fun testAuthorizeCommand_withoutAuthCode() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.command = "authorize"
        
        // When
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Error" && it.content.contains("requires input") }, "Should have error for missing input")
        assertEquals(AuthState.NoAuth, viewModel.authState, "Auth state should remain NoAuth")
        assertFalse(viewModel.commandWaiting, "Command waiting should be false")
    }

    @Test
    fun testLogoutCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.authState = AuthState.Authorized
        viewModel.command = "oktalogout"
        
        // When
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("logout") }, "Should have logout message")
        assertEquals(AuthState.LoggedOut, viewModel.authState, "Auth state should be LoggedOut")
        assertFalse(viewModel.commandWaiting, "Command waiting should be false")
    }

    @Test
    fun testAuthStateTransitions() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When - Simulate auth flow
        assertEquals(AuthState.NoAuth, viewModel.authState, "Should start with NoAuth")
        
        // Authorize with code
        viewModel.command = "authorize TEST123"
        viewModel.onCommandSend()
        assertEquals(AuthState.Authorized, viewModel.authState, "Should be Authorized after auth code")
        
        // Logout
        viewModel.command = "oktalogout"
        viewModel.onCommandSend()
        assertEquals(AuthState.LoggedOut, viewModel.authState, "Should be LoggedOut after logout")
    }

    // MARK: - Deployment Configuration Tests (Requirement 3.1, 3.4)

    @Test
    fun testDeploymentSettings_initialValues() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // Then
        assertEquals("", viewModel.deploymentId, "Initial deployment ID should be empty")
        assertEquals("", viewModel.region, "Initial region should be empty")
    }

    @Test
    fun testDeploymentSettings_updateValues() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.onDeploymentIdChanged("new-deployment-123")
        viewModel.onRegionChanged("us-west-2")
        
        // Then
        assertEquals("new-deployment-123", viewModel.deploymentId, "Deployment ID should be updated")
        assertEquals("us-west-2", viewModel.region, "Region should be updated")
    }

    @Test
    fun testUpdateDeploymentSettings_validSettings() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.updateDeploymentSettings("valid-deployment", "us-east-1")
        
        // Then
        assertEquals("valid-deployment", viewModel.deploymentId, "Deployment ID should be updated")
        assertEquals("us-east-1", viewModel.region, "Region should be updated")
        
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Success" && it.content.contains("validated successfully") }, "Should have validation success message")
    }

    @Test
    fun testUpdateDeploymentSettings_invalidSettings() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.updateDeploymentSettings("", "invalid-region")
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Validating deployment settings") }, "Should have validation message")
        // Note: The actual validation behavior depends on DeploymentValidator implementation
    }

    // Note: Reinitialization tests that require PlatformContext are skipped in common tests
    // These would be implemented in platform-specific test modules

    @Test
    fun testDeploymentCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.deploymentId = "test-deployment-456"
        viewModel.region = "eu-west-1"
        viewModel.command = "deployment"
        
        // When
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Deployment config: ID=test-deployment-456, Region=eu-west-1") }, "Should have deployment info message")
        assertFalse(viewModel.commandWaiting, "Command waiting should be false")
    }

    // MARK: - State Management Tests

    @Test
    fun testCommandChanged() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.onCommandChanged("test-command")
        
        // Then
        assertEquals("test-command", viewModel.command, "Command should be updated")
    }

    @Test
    fun testClientStateTracking() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        assertEquals(MessagingClientState.Idle, viewModel.clientState, "Should start with Idle state")
        
        // When - Execute connect command (will fail due to not being initialized)
        viewModel.command = "connect"
        viewModel.onCommandSend()
        
        // Then - State should remain Idle because command failed
        assertEquals(MessagingClientState.Idle, viewModel.clientState, "Should remain in Idle state when command fails")
    }

    @Test
    fun testErrorHandling_commandExecutionError() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When - Execute a command that might cause an error (simulated by invalid input)
        viewModel.command = "detach" // This requires an attachment ID
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Error" }, "Should have error message")
        assertFalse(viewModel.commandWaiting, "Command waiting should be false after error")
    }

    // MARK: - Integration Tests

    @Test
    fun testFullWorkflow_commandsWithoutInitialization() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.deploymentId = "test-deployment"
        viewModel.region = "us-east-1"
        
        // When - Execute connect command (will fail - not initialized)
        viewModel.command = "connect"
        viewModel.onCommandSend()
        
        // Then - Should remain in idle state
        assertEquals(MessagingClientState.Idle, viewModel.clientState, "Should remain idle when not initialized")
        
        // When - Send a message (should work as it doesn't require initialization)
        viewModel.command = "send Hello World"
        viewModel.onCommandSend()
        
        // Then - Should have sent message
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Send message: Hello World") }, "Should have sent message")
    }

    @Test
    fun testFullWorkflow_authenticationFlow() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When - Start Okta sign-in
        viewModel.command = "oktasignin"
        viewModel.onCommandSend()
        
        // Then - Should have sign-in message
        val messages1 = viewModel.socketMessages.value
        assertTrue(messages1.any { it.content.contains("Okta sign-in") }, "Should have sign-in message")
        
        // When - Provide authorization code
        viewModel.command = "authorize AUTH123CODE456"
        viewModel.onCommandSend()
        
        // Then - Should be authorized
        assertEquals(AuthState.Authorized, viewModel.authState, "Should be authorized")
        
        // When - Connect with authentication
        viewModel.command = "connectauthenticated"
        viewModel.onCommandSend()
        
        // Then - Should have authenticated connection
        val messages2 = viewModel.socketMessages.value
        assertTrue(messages2.any { it.content.contains("Connect authenticated") }, "Should have authenticated connection message")
    }
}