package com.genesys.cloud.messenger.composeapp.viewmodel

import com.genesys.cloud.messenger.composeapp.model.PlatformContext
import com.genesys.cloud.messenger.composeapp.model.SavedAttachment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test class for push notification commands in TestBedViewModel
 */
class TestBedViewModelPushTest {
    

    
    @Test
    fun testPushNotificationCommandsAreAvailable() {
        val viewModel = TestBedViewModel()
        val availableCommands = viewModel.availableCommands.value
        
        // Check that push notification commands are in the available commands list
        val commandNames = availableCommands.map { it.name.lowercase() }
        
        assertTrue(
            commandNames.contains("syncdevicetoken"),
            "syncdevicetoken command should be available"
        )
        
        assertTrue(
            commandNames.contains("unregpush"),
            "unregpush command should be available"
        )
    }
    
    @Test
    fun testSynchronizeDeviceTokenCommand() {
        val viewModel = TestBedViewModel()
        
        // Get initial message count
        val initialMessageCount = viewModel.socketMessages.value.size
        
        // Set the command and execute it
        viewModel.onCommandChanged("syncdevicetoken")
        viewModel.onCommandSend()
        
        // Verify that the command was executed (commandWaiting should be false after execution)
        assertFalse(viewModel.commandWaiting, "Command should complete execution")
        
        // Verify that socket messages were added
        val socketMessages = viewModel.socketMessages.value
        assertTrue(socketMessages.size > initialMessageCount, "Socket messages should be added")
        
        // Check that there's a message about synchronizing device token
        val hasTokenMessage = socketMessages.any { 
            it.content.contains("Synchronizing device token", ignoreCase = true) ||
            it.content.contains("Device token synchronized", ignoreCase = true)
        }
        assertTrue(hasTokenMessage, "Should have message about device token synchronization")
    }
    
    @Test
    fun testUnregisterFromPushCommand() {
        val viewModel = TestBedViewModel()
        
        // Get initial message count
        val initialMessageCount = viewModel.socketMessages.value.size
        
        // Set the command and execute it
        viewModel.onCommandChanged("unregpush")
        viewModel.onCommandSend()
        
        // Verify that the command was executed (commandWaiting should be false after execution)
        assertFalse(viewModel.commandWaiting, "Command should complete execution")
        
        // Verify that socket messages were added
        val socketMessages = viewModel.socketMessages.value
        assertTrue(socketMessages.size > initialMessageCount, "Socket messages should be added")
        
        // Check that there's a message about unregistering from push
        val hasUnregisterMessage = socketMessages.any { 
            it.content.contains("Unregistering from push", ignoreCase = true) ||
            it.content.contains("unregistered from push", ignoreCase = true)
        }
        assertTrue(hasUnregisterMessage, "Should have message about push unregistration")
    }
    
    @Test
    fun testCommandValidation() {
        val viewModel = TestBedViewModel()
        
        // Test that push notification commands are recognized as valid
        viewModel.onCommandChanged("syncdevicetoken")
        // This should not throw an error or show unknown command message
        
        viewModel.onCommandChanged("unregpush")
        // This should not throw an error or show unknown command message
        
        // The fact that we can set these commands without errors indicates they are valid
        assertEquals("unregpush", viewModel.command)
    }
}