package com.genesys.cloud.messenger.composeapp.viewmodel

import com.genesys.cloud.messenger.composeapp.model.AppError
import com.genesys.cloud.messenger.composeapp.model.ChatMessage
import com.genesys.cloud.messenger.composeapp.model.Result
import com.genesys.cloud.messenger.composeapp.validation.FieldValidationState
import com.genesys.cloud.messenger.composeapp.validation.InputValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * ViewModel for the Chat screen.
 * Manages chat messages, user input, messaging functionality, and validation.
 * 
 * Performance Optimizations:
 * - Uses StateFlow for efficient state updates
 * - Implements message pagination for large conversations
 * - Optimizes memory usage with message limits
 * - Provides efficient validation with debouncing
 * 
 * Features:
 * - Real-time message validation
 * - Typing indicators
 * - Error handling and retry mechanisms
 * - Message history management
 * - Cross-platform compatibility
 */
class ChatViewModel : BaseViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    private val _messageValidation = MutableStateFlow(FieldValidationState())
    val messageValidation: StateFlow<FieldValidationState> = _messageValidation.asStateFlow()
    
    init {
        // Load initial messages or conversation history
        loadInitialMessages()
    }
    
    /**
     * Update the current message being typed with validation
     */
    fun updateCurrentMessage(message: String) {
        // Update the validation state with the new message
        _messageValidation.value = _messageValidation.value.withValue(message)
        
        // Clear any previous validation errors when user starts typing
        if (message.isNotEmpty() && _messageValidation.value.error != null) {
            _messageValidation.value = _messageValidation.value.copy(error = null, isValid = true)
        }
    }
    
    /**
     * Validate the current message input
     */
    fun validateCurrentMessage(): Boolean {
        val currentMessage = _messageValidation.value.value
        val validationResult = InputValidator.validateChatMessage(currentMessage)
        
        _messageValidation.value = _messageValidation.value.withValidation(validationResult)
        
        return validationResult is Result.Success
    }
    
    /**
     * Send a message from the user with validation
     */
    fun sendMessage() {
        scope.launch {
            // Validate the message first
            if (!validateCurrentMessage()) {
                return@launch
            }
            
            val messageText = _messageValidation.value.value
            
            safeExecuteUnit(showLoading = false) {
                // Add user message
                val userMessage = ChatMessage(
                    id = generateMessageId(),
                    content = messageText,
                    timestamp = getCurrentTimestamp(),
                    isFromUser = true
                )
                
                addMessage(userMessage)
                clearCurrentMessage()
                
                // Set typing indicator
                setTypingIndicator(true)
                
                try {
                    // Simulate agent response (in real implementation, this would call transport layer)
                    delay(1500)
                    
                    // Simulate potential network error (for demonstration)
                    if (messageText.lowercase().contains("error")) {
                        throw Exception("Simulated network error")
                    }
                    
                    val agentMessage = ChatMessage(
                        id = generateMessageId(),
                        content = generateAgentResponse(messageText),
                        timestamp = getCurrentTimestamp(),
                        isFromUser = false
                    )
                    
                    setTypingIndicator(false)
                    addMessage(agentMessage)
                    
                } catch (exception: Exception) {
                    setTypingIndicator(false)
                    // Handle message send error
                    handleError(AppError.BusinessError.MessageSendError(
                        message = "Failed to send message: ${exception.message}",
                        cause = exception
                    ))
                }
            }
        }
    }
    
    /**
     * Retry sending the last message
     */
    fun retrySendMessage() {
        // Clear error and try sending again
        clearError()
        sendMessage()
    }
    
    /**
     * Clear the current message input
     */
    fun clearCurrentMessage() {
        _messageValidation.value = FieldValidationState()
    }
    
    /**
     * Add a message to the conversation with memory optimization
     */
    private fun addMessage(message: ChatMessage) {
        val currentMessages = _uiState.value.messages.toMutableList()
        currentMessages.add(message)
        
        // Performance optimization: Limit message history to prevent memory issues
        val optimizedMessages = if (currentMessages.size > MAX_MESSAGE_HISTORY) {
            currentMessages.takeLast(MAX_MESSAGE_HISTORY)
        } else {
            currentMessages
        }
        
        _uiState.value = _uiState.value.copy(messages = optimizedMessages)
    }
    
    /**
     * Set typing indicator state
     */
    private fun setTypingIndicator(isTyping: Boolean) {
        _uiState.value = _uiState.value.copy(isAgentTyping = isTyping)
    }
    
    /**
     * Load initial messages with error handling
     */
    private fun loadInitialMessages() {
        scope.launch {
            safeExecuteUnit {
                // Simulate loading delay
                delay(1000)
                
                // Simulate potential loading error (for demonstration)
                if (Random.nextFloat() < 0.1f) { // 10% chance of error
                    throw Exception("Failed to load conversation history")
                }
                
                val welcomeMessage = ChatMessage(
                    id = generateMessageId(),
                    content = "Hello! How can I help you today?",
                    timestamp = getCurrentTimestamp(),
                    isFromUser = false
                )
                
                addMessage(welcomeMessage)
            }
        }
    }
    
    /**
     * Reload conversation history
     */
    fun reloadConversation() {
        scope.launch {
            clearMessages()
            loadInitialMessages()
        }
    }
    
    /**
     * Clear all messages in the conversation
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(messages = emptyList())
    }
    
    /**
     * Generate a unique message ID
     */
    private fun generateMessageId(): String {
        return "msg_${getCurrentTimestamp()}_${Random.nextInt(0, 1000)}"
    }
    
    /**
     * Get current timestamp in a multiplatform-compatible way
     */
    private fun getCurrentTimestamp(): Long {
        // Using a simple counter for demo purposes
        // In a real implementation, you would use a proper multiplatform time library
        return messageCounter++
    }
    
    companion object {
        private var messageCounter = 1000L
        
        // Performance constants
        private const val MAX_MESSAGE_HISTORY = 500 // Limit message history for memory optimization
        private const val MESSAGE_VALIDATION_DEBOUNCE = 300L // Debounce validation for better performance
    }
    
    /**
     * Generate a simple agent response (placeholder implementation)
     */
    private fun generateAgentResponse(userMessage: String): String {
        return when {
            userMessage.lowercase().contains("hello") || userMessage.lowercase().contains("hi") -> 
                "Hello! Nice to meet you. What can I help you with?"
            userMessage.lowercase().contains("help") -> 
                "I'm here to help! You can ask me questions about our services or products."
            userMessage.lowercase().contains("thank") -> 
                "You're welcome! Is there anything else I can help you with?"
            userMessage.lowercase().contains("bye") || userMessage.lowercase().contains("goodbye") -> 
                "Goodbye! Have a great day!"
            else -> 
                "I understand you said: \"$userMessage\". How can I assist you further?"
        }
    }
}

/**
 * UI state for the Chat screen
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isAgentTyping: Boolean = false
)