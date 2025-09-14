package com.genesys.cloud.messenger.composeapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.genesys.cloud.messenger.composeapp.theme.CustomTextStyles

/**
 * A composable input field for typing and sending messages in the chat.
 *
 * @param value Current text value in the input field
 * @param onValueChange Callback when the text value changes
 * @param onSendMessage Callback when the send button is pressed
 * @param modifier Optional modifier for the component
 * @param placeholder Placeholder text to show when input is empty
 * @param enabled Whether the input field is enabled for interaction
 */
@Composable
fun InputField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Type a message...",
    enabled: Boolean = true
) {
    val canSend = value.text.isNotBlank() && enabled
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Text input field
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                placeholder = {
                    Text(
                        text = placeholder,
                        style = CustomTextStyles.ChatInputText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                textStyle = CustomTextStyles.ChatInputText,
                enabled = enabled,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                ),
                maxLines = 4,
                singleLine = false
            )
            
            // Send button
            FloatingActionButton(
                onClick = onSendMessage,
                modifier = Modifier.size(48.dp),
                containerColor = if (canSend) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (canSend) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                },
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = if (canSend) 6.dp else 2.dp
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}