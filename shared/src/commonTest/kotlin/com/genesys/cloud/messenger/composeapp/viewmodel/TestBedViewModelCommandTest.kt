package com.genesys.cloud.messenger.composeapp.viewmodel

import com.genesys.cloud.messenger.composeapp.model.AuthState
import com.genesys.cloud.messenger.composeapp.model.MessagingClientState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Detailed tests for command execution and state management in TestBedViewModel.
 * Covers all command categories and their state management behavior.
 * 
 * Requirements: 3.1, 3.2
 */
class TestBedViewModelCommandTest {

    // MARK: - Connection Commands

    @Test
    fun testConnectCommand_fromIdleState() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.deploymentId = "test-deployment"
        viewModel.region = "us-east-1"
        assertEquals(MessagingClientState.Idle, viewModel.clientState)
        
        // When
        viewModel.command = "connect"
        viewModel.onCommandSend()
        
        // Then
        assertEquals(MessagingClientState.Connecting, viewModel.clientState)
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Connect command executed") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testConnectAuthenticatedCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.authState = AuthState.Authorized
        
        // When
        viewModel.command = "connectauthenticated"
        viewModel.onCommandSend()
        
        // Then
        assertEquals(MessagingClientState.Connecting, viewModel.clientState)
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Connect authenticated command executed") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testDisconnectCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.clientState = MessagingClientState.Connected
        
        // When
        viewModel.command = "disconnect"
        viewModel.onCommandSend()
        
        // Then
        assertEquals(MessagingClientState.Idle, viewModel.clientState)
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Disconnect command executed") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testByeCommand_aliasForDisconnect() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.clientState = MessagingClientState.Connected
        
        // When
        viewModel.command = "bye"
        viewModel.onCommandSend()
        
        // Then
        assertEquals(MessagingClientState.Idle, viewModel.clientState)
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Disconnect command executed") })
        assertFalse(viewModel.commandWaiting)
    }

    // MARK: - Messaging Commands

    @Test
    fun testSendCommand_withValidMessage() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        val testMessage = "Hello, this is a test message!"
        
        // When
        viewModel.command = "send $testMessage"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Send message: $testMessage") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testSendCommand_withEmptyMessage() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "send"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Error" && it.content.contains("Message text is required") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testSendQuickReplyCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        val quickReplyText = "Option 1"
        
        // When
        viewModel.command = "sendquickreply $quickReplyText"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Quick reply sent: $quickReplyText") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testHealthCheckCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "healthcheck"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Health check sent") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testHistoryCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "history"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Fetching next page") })
        assertFalse(viewModel.commandWaiting)
    }

    // MARK: - Chat Management Commands

    @Test
    fun testStartNewChatCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "startnewchat"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("New chat started") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testNewChatCommand_aliasForStartNewChat() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "newchat"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("New chat started") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testClearConversationCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "clearconversation"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Conversation cleared") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testInvalidateConversationCacheCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "invalidateconversationcache"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Conversation cache invalidated") })
        assertFalse(viewModel.commandWaiting)
    }

    // MARK: - Attachment Commands

    @Test
    fun testAttachCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "attach"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("File attachment initiated") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testAttachSavedImageCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "attachsavedimage"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Saved image attachment") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testDetachCommand_withValidId() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        val attachmentId = "test-attachment-123"
        
        // When
        viewModel.command = "detach $attachmentId"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Error" && it.content.contains("Attachment not found") }) // Since no attachment exists
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testDetachCommand_withoutId() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "detach"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Error" && it.content.contains("Attachment ID is required") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testRefreshAttachmentCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        val attachmentId = "test-attachment-456"
        
        // When
        viewModel.command = "refreshattachment $attachmentId"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Refreshing attachment URL") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testChangeFileNameCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        val newFileName = "new-file-name.jpg"
        
        // When
        viewModel.command = "savedfilename $newFileName"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("File name changed to: $newFileName") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testFileAttachmentProfileCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "fileattachmentprofile"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("File attachment profile") })
        assertFalse(viewModel.commandWaiting)
    }

    // MARK: - Authentication Commands

    @Test
    fun testOktaSignInCommand_basic() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "oktasignin"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Okta sign-in initiated") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testOktaSignInWithPkceCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "oktasigninwithpkce"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Okta sign-in with PKCE initiated") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testAuthorizeCommand_validCode() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        val authCode = "AUTH123CODE456"
        
        // When
        viewModel.command = "authorize $authCode"
        viewModel.onCommandSend()
        
        // Then
        assertEquals(AuthState.Authorized, viewModel.authState)
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Processing authorization code") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testStepUpCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "stepup"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Step-up authentication") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testWasAuthenticatedCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.authState = AuthState.Authorized
        
        // When
        viewModel.command = "wasauthenticated"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Authentication status") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testShouldAuthorizeCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "shouldauthorize"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Authorization check") })
        assertFalse(viewModel.commandWaiting)
    }

    // MARK: - Token Management Commands

    @Test
    fun testRemoveTokenCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.authState = AuthState.Authorized
        
        // When
        viewModel.command = "removetoken"
        viewModel.onCommandSend()
        
        // Then
        assertEquals(AuthState.LoggedOut, viewModel.authState)
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Removing access token") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testRemoveAuthRefreshTokenCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.authState = AuthState.Authorized
        
        // When
        viewModel.command = "removeauthrefreshtoken"
        viewModel.onCommandSend()
        
        // Then
        assertEquals(AuthState.LoggedOut, viewModel.authState)
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Removing auth refresh token") })
        assertFalse(viewModel.commandWaiting)
    }

    // MARK: - Push Notification Commands

    @Test
    fun testSyncDeviceTokenCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "syncdevicetoken"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Synchronizing device token") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testUnregisterFromPushCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "unregpush"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Unregistering from push") })
        assertFalse(viewModel.commandWaiting)
    }

    // MARK: - Utility Commands

    @Test
    fun testDeploymentCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.deploymentId = "test-deployment-789"
        viewModel.region = "eu-central-1"
        
        // When
        viewModel.command = "deployment"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Deployment Configuration") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testAddCustomAttributesCommand_validInput() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "addattribute testKey testValue"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Custom attribute added") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testAddCustomAttributesCommand_invalidInput() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "addattribute"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Error" && it.content.contains("requires input") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testTypingCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "typing"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Typing indicator sent") })
        assertFalse(viewModel.commandWaiting)
    }

    // MARK: - Command State Management

    @Test
    fun testCommandWaitingState_duringExecution() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.command = "connect"
        
        // When - Set command waiting manually to test prevention
        viewModel.commandWaiting = true
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Warning" && it.content.contains("already in progress") })
    }

    @Test
    fun testCommandClearingAfterExecution() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.command = "deployment"
        
        // When
        viewModel.onCommandSend()
        
        // Then
        assertEquals("", viewModel.command, "Command should be cleared after execution")
        assertFalse(viewModel.commandWaiting, "Command waiting should be false after execution")
    }

    @Test
    fun testMultipleCommandsSequential() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When - Execute multiple commands in sequence
        viewModel.command = "deployment"
        viewModel.onCommandSend()
        
        viewModel.command = "typing"
        viewModel.onCommandSend()
        
        viewModel.command = "healthcheck"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Deployment Configuration") })
        assertTrue(messages.any { it.content.contains("Typing indicator sent") })
        assertTrue(messages.any { it.content.contains("Health check sent") })
        assertFalse(viewModel.commandWaiting, "Should not be waiting after all commands complete")
    }
}