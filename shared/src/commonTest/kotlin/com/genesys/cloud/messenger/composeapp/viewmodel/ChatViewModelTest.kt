package com.genesys.cloud.messenger.composeapp.viewmodel

import com.genesys.cloud.messenger.composeapp.model.AppError
import com.genesys.cloud.messenger.composeapp.util.TestDispatcherRule
import com.genesys.cloud.messenger.composeapp.validation.FieldValidationState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    
    private val testDispatcherRule = TestDispatcherRule()
    
    @BeforeTest
    fun setUp() {
        testDispatcherRule.setUp()
    }
    
    @AfterTest
    fun tearDown() {
        testDispatcherRule.tearDown()
    }
    
    @Test
    fun testInitialState() {
        val viewModel = ChatViewModel()
        
        val uiState = viewModel.uiState.value
        val messageValidation = viewModel.messageValidation.value
        
        assertTrue(uiState.messages.isEmpty())
        assertFalse(uiState.isAgentTyping)
        assertEquals("", messageValidation.value)
        assertNull(messageValidation.error)
        assertTrue(messageValidation.isValid)
    }
    
    @Test
    fun testUpdateCurrentMessage() {
        val viewModel = ChatViewModel()
        
        viewModel.updateCurrentMessage("Hello")
        
        val messageValidation = viewModel.messageValidation.value
        assertEquals("Hello", messageValidation.value)
        assertNull(messageValidation.error)
        assertTrue(messageValidation.isValid)
    }
    
    @Test
    fun testUpdateCurrentMessageClearsError() {
        val viewModel = ChatViewModel()
        
        // First, set an invalid message to trigger an error
        viewModel.updateCurrentMessage("")
        viewModel.validateCurrentMessage() // This should set an error
        
        // Verify error is set
        val errorState = viewModel.messageValidation.value
        assertNotNull(errorState.error)
        assertFalse(errorState.isValid)
        
        // Now update with valid content - this should clear the error due to withValue()
        viewModel.updateCurrentMessage("Valid message")
        
        val messageValidation = viewModel.messageValidation.value
        assertEquals("Valid message", messageValidation.value)
        assertNull(messageValidation.error) // withValue() clears the error
        assertTrue(messageValidation.isValid)
    }
    
    @Test
    fun testValidateCurrentMessageValid() {
        val viewModel = ChatViewModel()
        
        viewModel.updateCurrentMessage("Valid message")
        val isValid = viewModel.validateCurrentMessage()
        
        assertTrue(isValid)
        val messageValidation = viewModel.messageValidation.value
        assertNull(messageValidation.error)
        assertTrue(messageValidation.isValid)
    }
    
    @Test
    fun testValidateCurrentMessageEmpty() {
        val viewModel = ChatViewModel()
        
        viewModel.updateCurrentMessage("")
        val isValid = viewModel.validateCurrentMessage()
        
        assertFalse(isValid)
        val messageValidation = viewModel.messageValidation.value
        assertNotNull(messageValidation.error)
        assertTrue(messageValidation.error is AppError.ValidationError.EmptyFieldError)
        assertFalse(messageValidation.isValid)
    }
    
    @Test
    fun testValidateCurrentMessageTooLong() {
        val viewModel = ChatViewModel()
        
        // Create a message that's too long (over 4000 characters)
        val longMessage = "a".repeat(4001)
        viewModel.updateCurrentMessage(longMessage)
        val isValid = viewModel.validateCurrentMessage()
        
        assertFalse(isValid)
        val messageValidation = viewModel.messageValidation.value
        assertNotNull(messageValidation.error)
        assertTrue(messageValidation.error is AppError.ValidationError.TooLongError)
        assertFalse(messageValidation.isValid)
    }
    
    @Test
    fun testSendMessageSuccess() = runTest {
        val viewModel = ChatViewModel()
        
        viewModel.updateCurrentMessage("Hello")
        viewModel.sendMessage()
        
        // Wait a bit for the coroutine to complete
        kotlinx.coroutines.delay(100)
        
        val uiState = viewModel.uiState.value
        val messageValidation = viewModel.messageValidation.value
        
        // Should have at least one message (user message)
        assertTrue(uiState.messages.isNotEmpty())
        
        // Current message should be cleared
        assertEquals("", messageValidation.value)
        
        // Should not be in error state
        assertNull(viewModel.error.value)
    }
    
    @Test
    fun testSendMessageValidationFailure() = runTest {
        val viewModel = ChatViewModel()
        
        viewModel.updateCurrentMessage("") // Empty message
        viewModel.sendMessage()
        
        val uiState = viewModel.uiState.value
        val messageValidation = viewModel.messageValidation.value
        
        // Should not have sent any messages
        assertTrue(uiState.messages.isEmpty())
        
        // Should have validation error
        assertNotNull(messageValidation.error)
        assertFalse(messageValidation.isValid)
    }
    
    @Test
    fun testSendMessageWithErrorKeyword() = runTest {
        val viewModel = ChatViewModel()
        
        viewModel.updateCurrentMessage("error test")
        viewModel.sendMessage()
        
        // Wait for the operation to complete
        kotlinx.coroutines.delay(2000)
        
        val uiState = viewModel.uiState.value
        
        // Should have user message but error should be handled
        assertTrue(uiState.messages.isNotEmpty())
        
        // Should have an error due to the "error" keyword
        assertNotNull(viewModel.error.value)
        assertTrue(viewModel.error.value is AppError.BusinessError.MessageSendError)
    }
    
    @Test
    fun testClearCurrentMessage() {
        val viewModel = ChatViewModel()
        
        viewModel.updateCurrentMessage("Test message")
        viewModel.clearCurrentMessage()
        
        val messageValidation = viewModel.messageValidation.value
        assertEquals("", messageValidation.value)
        assertNull(messageValidation.error)
        assertTrue(messageValidation.isValid)
    }
    
    @Test
    fun testClearMessages() {
        val viewModel = ChatViewModel()
        
        // Add some messages first
        viewModel.updateCurrentMessage("Test")
        runTest {
            viewModel.sendMessage()
            kotlinx.coroutines.delay(100)
        }
        
        viewModel.clearMessages()
        
        val uiState = viewModel.uiState.value
        assertTrue(uiState.messages.isEmpty())
    }
    
    @Test
    fun testRetrySendMessage() = runTest {
        val viewModel = ChatViewModel()
        
        viewModel.updateCurrentMessage("Hello")
        viewModel.retrySendMessage()
        
        // Wait for operation
        kotlinx.coroutines.delay(100)
        
        // Should clear error and attempt to send
        assertNull(viewModel.error.value)
    }
    
    @Test
    fun testReloadConversation() = runTest {
        val viewModel = ChatViewModel()
        
        // Add some messages first
        viewModel.updateCurrentMessage("Test")
        viewModel.sendMessage()
        kotlinx.coroutines.delay(100)
        
        // Reload conversation
        viewModel.reloadConversation()
        kotlinx.coroutines.delay(1100) // Wait for reload to complete
        
        val uiState = viewModel.uiState.value
        
        // Should have welcome message after reload
        assertTrue(uiState.messages.isNotEmpty())
        assertEquals("Hello! How can I help you today?", uiState.messages.first().content)
        assertFalse(uiState.messages.first().isFromUser)
    }
}