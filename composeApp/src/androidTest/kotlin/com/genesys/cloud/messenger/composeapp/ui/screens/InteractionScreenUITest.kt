package com.genesys.cloud.messenger.composeapp.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.genesys.cloud.messenger.composeapp.model.*
import com.genesys.cloud.messenger.composeapp.viewmodel.TestBedViewModel
import com.genesys.cloud.messenger.composeapp.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

/**
 * Android-specific UI integration tests for InteractionScreen using Compose Test Framework.
 * These tests verify the actual UI behavior and user interactions.
 * 
 * Requirements addressed:
 * - 2.1: Display as "InteractionScreen" instead of "ChatScreen"
 * - 2.2: Display message type prominently
 * - 2.3: Expandable socket message details
 * - 2.4: Replace message input with command input field
 * - 2.5: Command dropdown with all available commands
 * - 2.6: Additional input field for commands requiring parameters
 */
class InteractionScreenUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test command dropdown functionality in UI
     * Requirements: 2.5, 4.1
     */
    @Test
    fun testCommandDropdownUI() {
        // Given
        val testBedViewModel = createTestBedViewModel()
        val settingsViewModel = createTestSettingsViewModel()
        
        // Set up test commands
        val testCommands = listOf(
            Command("connect", "Connect to messaging service", false),
            Command("send", "Send a message", true, "Enter message text"),
            Command("disconnect", "Disconnect from service", false)
        )
        setAvailableCommands(testBedViewModel, testCommands)
        
        // When
        composeTestRule.setContent {
            InteractionScreen(
                testBedViewModel = testBedViewModel,
                settingsViewModel = settingsViewModel,
                onNavigateBack = {}
            )
        }
        
        // Then - Verify command dropdown is displayed
        composeTestRule.onNodeWithText("Command").assertIsDisplayed()
        composeTestRule.onNodeWithText("Select or type a command").assertIsDisplayed()
        
        // When - Click on command dropdown
        composeTestRule.onNodeWithText("Command").performClick()
        
        // Then - Verify all commands are shown in dropdown
        composeTestRule.onNodeWithText("connect").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connect to messaging service").assertIsDisplayed()
        composeTestRule.onNodeWithText("send").assertIsDisplayed()
        composeTestRule.onNodeWithText("Send a message").assertIsDisplayed()
        composeTestRule.onNodeWithText("disconnect").assertIsDisplayed()
        composeTestRule.onNodeWithText("Disconnect from service").assertIsDisplayed()
    }

    /**
     * Test command selection and parameter input
     * Requirements: 2.6
     */
    @Test
    fun testCommandParameterInput() {
        // Given
        val testBedViewModel = createTestBedViewModel()
        val settingsViewModel = createTestSettingsViewModel()
        
        val testCommands = listOf(
            Command("send", "Send a message", true, "Enter message text"),
            Command("connect", "Connect to service", false)
        )
        setAvailableCommands(testBedViewModel, testCommands)
        
        composeTestRule.setContent {
            InteractionScreen(
                testBedViewModel = testBedViewModel,
                settingsViewModel = settingsViewModel,
                onNavigateBack = {}
            )
        }
        
        // When - Select command that requires parameters
        composeTestRule.onNodeWithText("Command").performClick()
        composeTestRule.onNodeWithText("send").performClick()
        
        // Then - Verify parameter input field appears
        composeTestRule.onNodeWithText("Parameters").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enter message text").assertIsDisplayed()
        
        // When - Enter parameter text
        composeTestRule.onNodeWithText("Parameters").performTextInput("Hello World")
        
        // Then - Verify parameter input is accepted
        composeTestRule.onNodeWithText("Hello World").assertIsDisplayed()
        
        // When - Select command that doesn't require parameters
        composeTestRule.onNodeWithText("Command").performClick()
        composeTestRule.onNodeWithText("connect").performClick()
        
        // Then - Verify parameter input field is hidden
        composeTestRule.onNodeWithText("Parameters").assertDoesNotExist()
    }

    /**
     * Test socket message display and expansion
     * Requirements: 2.2, 2.3, 5.1, 5.2
     */
    @Test
    fun testSocketMessageDisplayAndExpansion() {
        // Given
        val testBedViewModel = createTestBedViewModel()
        val settingsViewModel = createTestSettingsViewModel()
        
        val testMessage = SocketMessage(
            id = "test_msg",
            timestamp = 1234567890L,
            type = "Error",
            content = "Test message content",
            rawMessage = """{"type":"MessageEvent","data":"test content"}"""
        )
        
        composeTestRule.setContent {
            InteractionScreen(
                testBedViewModel = testBedViewModel,
                settingsViewModel = settingsViewModel,
                onNavigateBack = {}
            )
        }
        
        // When - Add socket message
        addSocketMessage(testBedViewModel, testMessage)
        
        // Then - Verify message type is displayed prominently (requirement 2.2)
        composeTestRule.onNodeWithText("MessageEvent").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test message content").assertIsDisplayed()
        
        // Verify collapse/expand icon is present
        composeTestRule.onNodeWithContentDescription("Expand").assertIsDisplayed()
        
        // When - Click to expand message (requirement 2.3)
        composeTestRule.onNodeWithText("MessageEvent").performClick()
        
        // Then - Verify expanded content is shown
        composeTestRule.onNodeWithContentDescription("Collapse").assertIsDisplayed()
        composeTestRule.onNodeWithText("""{"type":"MessageEvent","data":"test content"}""").assertIsDisplayed()
        
        // When - Click to collapse message
        composeTestRule.onNodeWithText("MessageEvent").performClick()
        
        // Then - Verify message is collapsed
        composeTestRule.onNodeWithContentDescription("Expand").assertIsDisplayed()
        composeTestRule.onNodeWithText("""{"type":"MessageEvent","data":"test content"}""").assertDoesNotExist()
    }

    /**
     * Test command execution and loading states
     * Requirements: 4.2, 4.3
     */
    @Test
    fun testCommandExecutionLoadingStates() {
        // Given
        val testBedViewModel = createTestBedViewModel()
        val settingsViewModel = createTestSettingsViewModel()
        
        val testCommands = listOf(
            Command("connect", "Connect to service", false)
        )
        setAvailableCommands(testBedViewModel, testCommands)
        
        composeTestRule.setContent {
            InteractionScreen(
                testBedViewModel = testBedViewModel,
                settingsViewModel = settingsViewModel,
                onNavigateBack = {}
            )
        }
        
        // When - Select and execute command
        composeTestRule.onNodeWithText("Command").performClick()
        composeTestRule.onNodeWithText("connect").performClick()
        
        // Verify execute button is enabled
        composeTestRule.onNodeWithText("Execute").assertIsDisplayed()
        composeTestRule.onNodeWithText("Execute").assertIsEnabled()
        
        // When - Simulate command execution loading state
        setCommandWaiting(testBedViewModel, true)
        
        // Then - Verify loading state UI (requirement 4.2)
        composeTestRule.onNodeWithText("Executing...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Executing Command").assertIsDisplayed()
        
        // Verify execute button shows loading state
        composeTestRule.onNodeWithText("Executing...").assertIsDisplayed()
        
        // When - Command execution completes
        setCommandWaiting(testBedViewModel, false)
        
        // Then - Verify loading state is cleared
        composeTestRule.onNodeWithText("Execute").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ready to execute commands").assertIsDisplayed()
    }

    /**
     * Test initialization flow UI states
     * Requirements: 3.1
     */
    @Test
    fun testInitializationFlowUI() {
        // Given
        val testBedViewModel = createTestBedViewModel()
        val settingsViewModel = createTestSettingsViewModel()
        
        composeTestRule.setContent {
            InteractionScreen(
                testBedViewModel = testBedViewModel,
                settingsViewModel = settingsViewModel,
                onNavigateBack = {}
            )
        }
        
        // Then - Verify initial state (not initialized)
        composeTestRule.onNodeWithText("Not Initialized").assertIsDisplayed()
        composeTestRule.onNodeWithText("Waiting for settings").assertIsDisplayed()
        
        // When - Set valid deployment settings
        updateSettings(settingsViewModel, "test-deployment", "us-east-1")
        
        // Then - Verify settings are reflected in UI
        composeTestRule.onNodeWithText("test-deployment • us-east-1").assertIsDisplayed()
        
        // When - Simulate initialization error
        setInitializationError(testBedViewModel, "Failed to initialize: Invalid deployment")
        
        // Then - Verify error state is shown
        composeTestRule.onNodeWithText("Initialization Error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Failed").assertIsDisplayed()
    }

    /**
     * Test client state indicators
     * Requirements: 4.3, 4.4
     */
    @Test
    fun testClientStateIndicators() {
        // Given
        val testBedViewModel = createTestBedViewModel()
        val settingsViewModel = createTestSettingsViewModel()
        
        composeTestRule.setContent {
            InteractionScreen(
                testBedViewModel = testBedViewModel,
                settingsViewModel = settingsViewModel,
                onNavigateBack = {}
            )
        }
        
        // Test different client states
        val stateTests = mapOf(
            MessagingClientState.Idle to "Idle",
            MessagingClientState.Connecting to "Connecting",
            MessagingClientState.Connected to "Connected",
            MessagingClientState.Disconnecting to "Disconnecting",
            MessagingClientState.Disconnected to "Disconnected",
            MessagingClientState.Error to "Error"
        )
        
        stateTests.forEach { (state, expectedText) ->
            // When - Set client state
            setClientState(testBedViewModel, state)
            
            // Then - Verify state is displayed
            composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
            
            // Verify appropriate status text
            when (state) {
                MessagingClientState.Connected -> {
                    composeTestRule.onNodeWithText("Ready").assertIsDisplayed()
                }
                MessagingClientState.Error -> {
                    composeTestRule.onNodeWithText("Error").assertIsDisplayed()
                }
                MessagingClientState.Idle -> {
                    composeTestRule.onNodeWithText("Ready").assertIsDisplayed()
                }
                else -> {
                    // Other states show "Busy" or specific status
                    composeTestRule.onNodeWithText("Busy").assertIsDisplayed()
                }
            }
        }
    }

    /**
     * Test empty state display
     * Requirements: 5.1
     */
    @Test
    fun testEmptyStateDisplay() {
        // Given
        val testBedViewModel = createTestBedViewModel()
        val settingsViewModel = createTestSettingsViewModel()
        
        composeTestRule.setContent {
            InteractionScreen(
                testBedViewModel = testBedViewModel,
                settingsViewModel = settingsViewModel,
                onNavigateBack = {}
            )
        }
        
        // Then - Verify empty state is shown when no messages
        composeTestRule.onNodeWithText("TestBed Ready").assertIsDisplayed()
        composeTestRule.onNodeWithText("Socket messages and command results will appear here. Execute commands to start testing the messaging client.").assertIsDisplayed()
        composeTestRule.onNodeWithText("🔧").assertIsDisplayed()
    }

    /**
     * Test error display and clearing
     * Requirements: 4.5
     */
    @Test
    fun testErrorDisplayAndClearing() {
        // Given
        val testBedViewModel = createTestBedViewModel()
        val settingsViewModel = createTestSettingsViewModel()
        
        composeTestRule.setContent {
            InteractionScreen(
                testBedViewModel = testBedViewModel,
                settingsViewModel = settingsViewModel,
                onNavigateBack = {}
            )
        }
        
        // When - Add an error
        val testError = TestBedError.CommandExecutionError.InvalidCommandError(
            message = "Invalid command 'badcommand'",
            command = "badcommand"
        )
        addError(testBedViewModel, testError)
        
        // Then - Verify error is displayed
        composeTestRule.onNodeWithText("1 Error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Command Error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clear").assertIsDisplayed()
        
        // When - Expand error details
        composeTestRule.onNodeWithText("1 Error").performClick()
        
        // Then - Verify expanded error details
        composeTestRule.onNodeWithText("Latest Error:").assertIsDisplayed()
        composeTestRule.onNodeWithText("Invalid command 'badcommand'").assertIsDisplayed()
        composeTestRule.onNodeWithText("Command: badcommand").assertIsDisplayed()
        
        // When - Clear errors
        composeTestRule.onNodeWithText("Clear").performClick()
        
        // Then - Verify errors are cleared
        composeTestRule.onNodeWithText("1 Error").assertDoesNotExist()
        composeTestRule.onNodeWithText("Clear").assertDoesNotExist()
    }

    /**
     * Test navigation and screen title
     * Requirements: 2.1
     */
    @Test
    fun testScreenTitleAndNavigation() {
        // Given
        val testBedViewModel = createTestBedViewModel()
        val settingsViewModel = createTestSettingsViewModel()
        var backNavigationCalled = false
        
        composeTestRule.setContent {
            InteractionScreen(
                testBedViewModel = testBedViewModel,
                settingsViewModel = settingsViewModel,
                onNavigateBack = { backNavigationCalled = true }
            )
        }
        
        // Then - Verify screen is displayed as "InteractionScreen" (requirement 2.1)
        composeTestRule.onNodeWithText("TestBed Interaction").assertIsDisplayed()
        
        // Verify back navigation works
        composeTestRule.onNodeWithContentDescription("Navigate back").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        
        // Verify navigation callback was called
        assert(backNavigationCalled) { "Back navigation should have been called" }
    }

    // Helper functions for test setup
    private fun createTestBedViewModel(): TestBedViewModel {
        return TestBedViewModel()
    }

    private fun createTestSettingsViewModel(): SettingsViewModel {
        return SettingsViewModel()
    }

    private fun setAvailableCommands(viewModel: TestBedViewModel, commands: List<Command>) {
        val mutableFlow = viewModel.availableCommands as? MutableStateFlow<List<Command>>
        mutableFlow?.value = commands
    }

    private fun addSocketMessage(viewModel: TestBedViewModel, message: SocketMessage) {
        viewModel.addSocketMessage(message)
    }

    private fun setCommandWaiting(viewModel: TestBedViewModel, waiting: Boolean) {
        viewModel.commandWaiting = waiting
    }

    private fun setClientState(viewModel: TestBedViewModel, state: MessagingClientState) {
        viewModel.clientState = state
    }

    private fun updateSettings(viewModel: SettingsViewModel, deploymentId: String, region: String) {
        viewModel.updateDeploymentId(deploymentId)
        viewModel.updateRegion(region)
    }

    private fun setInitializationError(viewModel: TestBedViewModel, error: String) {
        // This would typically be set through the initialization process
        // For testing, we simulate the error state
        viewModel.addSocketMessage(
            SocketMessage(
                id = "init_error",
                timestamp = System.currentTimeMillis(),
                type = "InitializationError",
                content = error,
                rawMessage = "Initialization Error: $error"
            )
        )
    }

    private fun addError(viewModel: TestBedViewModel, error: TestBedError) {
        val currentErrors = viewModel.errorHistory.value.toMutableList()
        currentErrors.add(error)
        
        val mutableErrorHistory = viewModel.errorHistory as? MutableStateFlow<List<TestBedError>>
        val mutableLastError = viewModel.lastError as? MutableStateFlow<TestBedError?>
        
        mutableErrorHistory?.value = currentErrors
        mutableLastError?.value = error
    }
}