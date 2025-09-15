package com.genesys.cloud.messenger.composeapp.viewmodel

import com.genesys.cloud.messenger.composeapp.model.AuthState
import com.genesys.cloud.messenger.composeapp.model.MessagingClientState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Test class for TestBedViewModel utility command functionality
 */
class TestBedViewModelUtilityTest {

    @Test
    fun testDeploymentCommand() {
        val viewModel = TestBedViewModel()
        
        // Set some deployment configuration
        viewModel.onDeploymentIdChanged("test-deployment-123")
        viewModel.onRegionChanged("us-west-2")
        
        // Get initial message count
        val initialMessageCount = viewModel.socketMessages.value.size
        
        // Execute the deployment command
        viewModel.onCommandChanged("deployment")
        viewModel.onCommandSend()
        
        // Verify that messages were added
        val messages = viewModel.socketMessages.value
        assertTrue(messages.size > initialMessageCount, "Should have more socket messages after command execution")
        
        // Verify that there's a message about deployment configuration
        val hasDeploymentMessage = messages.any { 
            it.content.contains("Deployment Configuration", ignoreCase = true) ||
            it.content.contains("test-deployment-123") ||
            it.content.contains("us-west-2")
        }
        assertTrue(hasDeploymentMessage, "Should have message about deployment configuration")
        
        // Verify command waiting state is reset
        assertFalse(viewModel.commandWaiting, "Command waiting should be false after completion")
    }

    @Test
    fun testRemoveTokenFromVaultCommand() {
        val viewModel = TestBedViewModel()
        
        // Set auth state to Authorized to test state change
        viewModel.authState = AuthState.Authorized
        
        // Get initial message count
        val initialMessageCount = viewModel.socketMessages.value.size
        
        // Execute the removetoken command
        viewModel.onCommandChanged("removetoken")
        viewModel.onCommandSend()
        
        // Verify that messages were added
        val messages = viewModel.socketMessages.value
        assertTrue(messages.size > initialMessageCount, "Should have more socket messages after command execution")
        
        // Verify that there's a message about token removal
        val hasTokenMessage = messages.any { 
            it.content.contains("Removing access token", ignoreCase = true) ||
            it.content.contains("token removed", ignoreCase = true)
        }
        assertTrue(hasTokenMessage, "Should have message about token removal")
        
        // Verify auth state changed to LoggedOut
        assertEquals(AuthState.LoggedOut, viewModel.authState, "Auth state should be LoggedOut after token removal")
        
        // Verify command waiting state is reset
        assertFalse(viewModel.commandWaiting, "Command waiting should be false after completion")
    }

    @Test
    fun testRemoveAuthRefreshTokenFromVaultCommand() {
        val viewModel = TestBedViewModel()
        
        // Set auth state to Authorized to test state change
        viewModel.authState = AuthState.Authorized
        
        // Get initial message count
        val initialMessageCount = viewModel.socketMessages.value.size
        
        // Execute the removeauthrefreshtoken command
        viewModel.onCommandChanged("removeauthrefreshtoken")
        viewModel.onCommandSend()
        
        // Verify that messages were added
        val messages = viewModel.socketMessages.value
        assertTrue(messages.size > initialMessageCount, "Should have more socket messages after command execution")
        
        // Verify that there's a message about refresh token removal
        val hasRefreshTokenMessage = messages.any { 
            it.content.contains("Removing auth refresh token", ignoreCase = true) ||
            it.content.contains("refresh token removed", ignoreCase = true)
        }
        assertTrue(hasRefreshTokenMessage, "Should have message about refresh token removal")
        
        // Verify auth state changed to LoggedOut
        assertEquals(AuthState.LoggedOut, viewModel.authState, "Auth state should be LoggedOut after refresh token removal")
        
        // Verify command waiting state is reset
        assertFalse(viewModel.commandWaiting, "Command waiting should be false after completion")
    }

    @Test
    fun testAddCustomAttributesCommand() {
        val viewModel = TestBedViewModel()
        
        // Get initial message count
        val initialMessageCount = viewModel.socketMessages.value.size
        
        // Execute the addattribute command with key-value pair
        viewModel.onCommandChanged("addattribute testKey testValue")
        viewModel.onCommandSend()
        
        // Verify that messages were added
        val messages = viewModel.socketMessages.value
        assertTrue(messages.size > initialMessageCount, "Should have more socket messages after command execution")
        
        // Verify that there's a message about custom attribute addition
        val hasAttributeMessage = messages.any { 
            it.content.contains("Custom attribute added", ignoreCase = true) &&
            it.content.contains("testKey") &&
            it.content.contains("testValue")
        }
        assertTrue(hasAttributeMessage, "Should have message about custom attribute addition")
        
        // Verify command waiting state is reset
        assertFalse(viewModel.commandWaiting, "Command waiting should be false after completion")
    }

    @Test
    fun testAddCustomAttributesCommandWithEmptyInput() {
        val viewModel = TestBedViewModel()
        
        // Get initial message count
        val initialMessageCount = viewModel.socketMessages.value.size
        
        // Execute the addattribute command without arguments
        viewModel.onCommandChanged("addattribute")
        viewModel.onCommandSend()
        
        // Verify that messages were added
        val messages = viewModel.socketMessages.value
        assertTrue(messages.size > initialMessageCount, "Should have more socket messages after command execution")
        
        // Verify that there's an error message about missing input
        val hasErrorMessage = messages.any { 
            it.type == "Error" && 
            it.content.contains("requires input", ignoreCase = true)
        }
        assertTrue(hasErrorMessage, "Should have error message about missing input")
    }

    @Test
    fun testIndicateTypingCommand() {
        val viewModel = TestBedViewModel()
        
        // Get initial message count
        val initialMessageCount = viewModel.socketMessages.value.size
        
        // Execute the typing command
        viewModel.onCommandChanged("typing")
        viewModel.onCommandSend()
        
        // Verify that messages were added
        val messages = viewModel.socketMessages.value
        assertTrue(messages.size > initialMessageCount, "Should have more socket messages after command execution")
        
        // Verify that there's a message about typing indicator
        val hasTypingMessage = messages.any { 
            it.content.contains("Typing indicator sent", ignoreCase = true)
        }
        assertTrue(hasTypingMessage, "Should have message about typing indicator")
        
        // Verify command waiting state is reset
        assertFalse(viewModel.commandWaiting, "Command waiting should be false after completion")
    }

    @Test
    fun testInvalidateConversationCacheCommand() {
        val viewModel = TestBedViewModel()
        
        // Get initial message count
        val initialMessageCount = viewModel.socketMessages.value.size
        
        // Execute the invalidateconversationcache command
        viewModel.onCommandChanged("invalidateconversationcache")
        viewModel.onCommandSend()
        
        // Verify that messages were added
        val messages = viewModel.socketMessages.value
        assertTrue(messages.size > initialMessageCount, "Should have more socket messages after command execution")
        
        // Verify that there's a message about cache invalidation
        val hasCacheMessage = messages.any { 
            it.content.contains("Conversation cache invalidated", ignoreCase = true)
        }
        assertTrue(hasCacheMessage, "Should have message about cache invalidation")
        
        // Verify command waiting state is reset
        assertFalse(viewModel.commandWaiting, "Command waiting should be false after completion")
    }

    @Test
    fun testUtilityCommandsAreAvailable() {
        val viewModel = TestBedViewModel()
        
        val availableCommands = viewModel.availableCommands.value
        val commandNames = availableCommands.map { it.name.lowercase() }
        
        // Verify all utility commands are available
        assertTrue(commandNames.contains("deployment"), "deployment command should be available")
        assertTrue(commandNames.contains("addattribute"), "addattribute command should be available")
        assertTrue(commandNames.contains("typing"), "typing command should be available")
        assertTrue(commandNames.contains("removetoken"), "removetoken command should be available")
        assertTrue(commandNames.contains("removeauthrefreshtoken"), "removeauthrefreshtoken command should be available")
        assertTrue(commandNames.contains("invalidateconversationcache"), "invalidateconversationcache command should be available")
    }

    @Test
    fun testTokenRemovalWithNoAuthState() {
        val viewModel = TestBedViewModel()
        
        // Ensure auth state is NoAuth (default)
        assertEquals(AuthState.NoAuth, viewModel.authState, "Initial auth state should be NoAuth")
        
        // Get initial message count
        val initialMessageCount = viewModel.socketMessages.value.size
        
        // Execute the removetoken command
        viewModel.onCommandChanged("removetoken")
        viewModel.onCommandSend()
        
        // Verify that messages were added
        val messages = viewModel.socketMessages.value
        assertTrue(messages.size > initialMessageCount, "Should have more socket messages after command execution")
        
        // Verify that there's a message about token removal
        val hasTokenMessage = messages.any { 
            it.content.contains("token removed", ignoreCase = true)
        }
        assertTrue(hasTokenMessage, "Should have message about token removal")
        
        // Verify auth state remains NoAuth (no change needed)
        assertEquals(AuthState.NoAuth, viewModel.authState, "Auth state should remain NoAuth when no token was present")
    }
}