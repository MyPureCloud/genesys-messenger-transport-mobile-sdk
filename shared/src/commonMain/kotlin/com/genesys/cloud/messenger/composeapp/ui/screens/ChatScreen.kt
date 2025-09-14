package com.genesys.cloud.messenger.composeapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.genesys.cloud.messenger.composeapp.ui.components.ChatTopBar
import com.genesys.cloud.messenger.composeapp.ui.components.InputField
import com.genesys.cloud.messenger.composeapp.ui.components.MessageBubble
import com.genesys.cloud.messenger.composeapp.viewmodel.ChatViewModel
import com.genesys.cloud.messenger.composeapp.viewmodel.ChatUiState
import kotlinx.coroutines.delay

/**
 * Chat screen composable that displays the messaging interface.
 * Shows conversation messages and provides input for sending new messages.
 *
 * Requirements addressed:
 * - 2.1: Shared UI components using Compose Multiplatform
 * - 4.1: Basic navigation between screens
 * - 4.3: User interaction handling
 *
 * @param chatViewModel ViewModel for managing chat state and messages
 * @param onNavigateBack Callback when user wants to navigate back
 * @param modifier Optional modifier for the component
 */
@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by chatViewModel.uiState.collectAsState()
    val currentMessage by chatViewModel.currentMessage.collectAsState()
    
    var textFieldValue by remember { 
        mutableStateOf(TextFieldValue(currentMessage)) 
    }
    
    // Update text field when ViewModel state changes
    LaunchedEffect(currentMessage) {
        if (textFieldValue.text != currentMessage) {
            textFieldValue = TextFieldValue(currentMessage)
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top bar
        ChatTopBar(
            title = "Chat",
            subtitle = if (uiState.isAgentTyping) "Agent is typing..." else "Online",
            onBackClick = onNavigateBack
        )
        
        // Chat content
        ChatContent(
            uiState = uiState,
            textFieldValue = textFieldValue,
            onTextFieldValueChange = { newValue ->
                textFieldValue = newValue
                chatViewModel.updateCurrentMessage(newValue.text)
            },
            onSendMessage = {
                chatViewModel.sendMessage()
                textFieldValue = TextFieldValue("")
            },
            onRetry = {
                chatViewModel.clearError()
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Main content of the chat screen
 */
@Composable
private fun ChatContent(
    uiState: ChatUiState,
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    onSendMessage: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Messages area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                uiState.isLoading && uiState.messages.isEmpty() -> {
                    // Initial loading state
                    LoadingState(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.messages.isEmpty() -> {
                    // Empty state
                    EmptyState(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    // Messages list
                    MessagesList(
                        messages = uiState.messages,
                        isAgentTyping = uiState.isAgentTyping,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // Error overlay
            uiState.error?.let { error ->
                ErrorOverlay(
                    error = error,
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        
        // Input area
        InputField(
            value = textFieldValue,
            onValueChange = onTextFieldValueChange,
            onSendMessage = onSendMessage,
            enabled = !uiState.isLoading,
            placeholder = "Type your message..."
        )
    }
}

/**
 * Messages list with auto-scroll to bottom
 */
@Composable
private fun MessagesList(
    messages: List<com.genesys.cloud.messenger.composeapp.model.ChatMessage>,
    isAgentTyping: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, isAgentTyping) {
        if (messages.isNotEmpty()) {
            delay(100) // Small delay to ensure layout is complete
            listState.animateScrollToItem(
                if (isAgentTyping) messages.size else messages.size - 1
            )
        }
    }
    
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(
            items = messages,
            key = { message -> message.id }
        ) { message ->
            MessageBubble(
                message = message,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Typing indicator
        if (isAgentTyping) {
            item {
                TypingIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Typing indicator for when agent is typing
 */
@Composable
private fun TypingIndicator(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated dots
                repeat(3) { index ->
                    var alpha by remember { mutableStateOf(0.3f) }
                    
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(index * 200L)
                            alpha = 1f
                            delay(600)
                            alpha = 0.3f
                            delay(600)
                        }
                    }
                    
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(48.dp))
    }
}

/**
 * Loading state for initial chat loading
 */
@Composable
private fun LoadingState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Loading chat...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Empty state when no messages are present
 */
@Composable
private fun EmptyState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "💬",
            style = MaterialTheme.typography.displayMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Start a conversation",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Send a message to begin chatting with our support team.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Error overlay for displaying chat errors
 */
@Composable
private fun ErrorOverlay(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onRetry) {
                    Text(
                        text = "Retry",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}