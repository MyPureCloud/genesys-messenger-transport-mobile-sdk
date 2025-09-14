package com.genesys.cloud.messenger.composeapp.viewmodel

import com.genesys.cloud.messenger.composeapp.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * ViewModel for the Chat screen.
 * Manages chat messages, user input, and messaging functionality.
 */
class ChatViewModel : BaseViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    private val _currentMessage = MutableStateFlow("")
    val currentMessage: StateFlow<String> = _currentMessage.asStateFlow()
    
    init {
        // Load initial messages or conversation history
        loadInitialMessages()
    }
    
    /**
     * Update the current message being typed
     */
    fun updateCurrentMessage(message: String) {
        _currentMessage.value = message
    }
    
    /**
     * Send a message from the user
     */
    fun sendMessage() {
        val messageText = _currentMessage.value.trim()
        if (messageText.isEmpty()) return
        
        scope.launch {
            // Add user message
            val userMessage = ChatMessage(
                id = generateMessageId(),
                content = messageText,
                timestamp = getCurrentTimestamp(),
                isFromUser = true
            )
            
            addMessage(userMessage)
            _currentMessage.value = ""
            
            // Set typing indicator
            setTypingIndicator(true)
            
            // Simulate agent response (in real implementation, this would call transport layer)
            delay(1500)
            
            val agentMessage = ChatMessage(
                id = generateMessageId(),
                content = generateAgentResponse(messageText),
                timestamp = getCurrentTimestamp(),
                isFromUser = false
            )
            
            setTypingIndicator(false)
            addMessage(agentMessage)
        }
    }
    
    /**
     * Add a message to the conversation
     */
    private fun addMessage(message: ChatMessage) {
        val currentMessages = _uiState.value.messages.toMutableList()
        currentMessages.add(message)
        _uiState.value = _uiState.value.copy(messages = currentMessages)
    }
    
    /**
     * Set typing indicator state
     */
    private fun setTypingIndicator(isTyping: Boolean) {
        _uiState.value = _uiState.value.copy(isAgentTyping = isTyping)
    }
    
    /**
     * Load initial messages (placeholder implementation)
     */
    private fun loadInitialMessages() {
        scope.launch {
            setLoading(true)
            
            // Simulate loading delay
            delay(1000)
            
            val welcomeMessage = ChatMessage(
                id = generateMessageId(),
                content = "Hello! How can I help you today?",
                timestamp = getCurrentTimestamp(),
                isFromUser = false
            )
            
            addMessage(welcomeMessage)
            setLoading(false)
        }
    }
    
    /**
     * Set loading state
     */
    private fun setLoading(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = isLoading)
    }
    
    /**
     * Set error message
     */
    fun setError(error: String?) {
        _uiState.value = _uiState.value.copy(error = error)
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
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
    val isLoading: Boolean = false,
    val isAgentTyping: Boolean = false,
    val error: String? = null
)