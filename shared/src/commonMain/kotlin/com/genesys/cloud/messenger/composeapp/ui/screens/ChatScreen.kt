package com.genesys.cloud.messenger.composeapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
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
    val messageValidation by chatViewModel.messageValidation.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val error by chatViewModel.error.collectAsState()
    
    var textFieldValue by remember { 
        mutableStateOf(TextFieldValue(messageValidation.value)) 
    }
    
    // Update text field when ViewModel state changes
    LaunchedEffect(messageValidation.value) {
        if (textFieldValue.text != messageValidation.value) {
            textFieldValue = TextFieldValue(messageValidation.value)
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
            messageValidation = messageValidation,
            isLoading = isLoading,
            error = error,
            onTextFieldValueChange = { newValue ->
                textFieldValue = newValue
                chatViewModel.updateCurrentMessage(newValue.text)
            },
            onSendMessage = {
                chatViewModel.sendMessage()
                // Don't clear text field here - let ViewModel handle it after successful send
            },
            onRetry = {
                chatViewModel.retrySendMessage()
            },
            onDismissError = {
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
    messageValidation: com.genesys.cloud.messenger.composeapp.validation.FieldValidationState,
    isLoading: Boolean,
    error: com.genesys.cloud.messenger.composeapp.model.AppError?,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    onSendMessage: () -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
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
                isLoading && uiState.messages.isEmpty() -> {
                    // Initial loading state
                    LoadingState(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.messages.isEmpty() && !isLoading -> {
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
            
            // Error display using new error components
            error?.let { appError ->
                com.genesys.cloud.messenger.composeapp.ui.components.ErrorSnackbar(
                    error = appError,
                    onDismiss = onDismissError,
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        
        // Input area with validation
        Column {
            // Show validation error if present
            com.genesys.cloud.messenger.composeapp.ui.components.InlineErrorDisplay(
                error = messageValidation.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            InputField(
                value = textFieldValue,
                onValueChange = onTextFieldValueChange,
                onSendMessage = onSendMessage,
                enabled = !isLoading,
                placeholder = "Type your message...",
                isError = messageValidation.error != null
            )
        }
    }
}

/**
 * Messages list with auto-scroll to bottom
 * Optimized for performance with proper key handling and scroll behavior
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessagesList(
    messages: List<com.genesys.cloud.messenger.composeapp.model.ChatMessage>,
    isAgentTyping: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new messages arrive - optimized to reduce unnecessary scrolling
    LaunchedEffect(messages.size, isAgentTyping) {
        if (messages.isNotEmpty()) {
            // Check if user is already at the bottom before auto-scrolling
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = if (isAgentTyping) messages.size + 1 else messages.size
            val isNearBottom = lastVisibleIndex >= totalItems - 2
            
            if (isNearBottom) {
                delay(50) // Reduced delay for better responsiveness
                listState.animateScrollToItem(
                    if (isAgentTyping) messages.size else messages.size - 1
                )
            }
        }
    }
    
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        // Performance optimization: reverse layout for better scroll performance with large lists
        reverseLayout = false
    ) {
        items(
            items = messages,
            key = { message -> message.id }, // Stable keys for better performance
            contentType = { "message" } // Content type for better recycling
        ) { message ->
            MessageBubble(
                message = message,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItemPlacement() // Smooth animations for item changes
            )
        }
        
        // Typing indicator with stable key
        if (isAgentTyping) {
            item(
                key = "typing_indicator",
                contentType = "typing"
            ) {
                TypingIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItemPlacement()
                )
            }
        }
    }
}

/**
 * Typing indicator for when agent is typing
 * Optimized animation with better performance and memory usage
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
                // Optimized animated dots with shared animation state
                val infiniteTransition = rememberInfiniteTransition(label = "typing")
                
                repeat(3) { index ->
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 600,
                                delayMillis = index * 200,
                                easing = LinearEasing
                            ),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_$index"
                    )
                    
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

