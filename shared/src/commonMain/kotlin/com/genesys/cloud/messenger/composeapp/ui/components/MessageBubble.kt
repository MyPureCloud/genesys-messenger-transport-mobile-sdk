package com.genesys.cloud.messenger.composeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.genesys.cloud.messenger.composeapp.model.ChatMessage
import com.genesys.cloud.messenger.composeapp.theme.*


/**
 * A composable that displays a chat message bubble with appropriate styling
 * based on whether it's from the user or the agent/bot.
 *
 * @param message The chat message to display
 * @param modifier Optional modifier for the component
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isFromUser = message.isFromUser
    
    // Determine bubble colors based on theme and sender
    val bubbleColor = if (isFromUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = if (isFromUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    // Format timestamp - simple approach for multiplatform compatibility
    val formattedTime = formatTimestamp(message.timestamp)
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (isFromUser) Arrangement.End else Arrangement.Start
    ) {
        // Add spacing for user messages to push them to the right
        if (isFromUser) {
            Spacer(modifier = Modifier.width(48.dp))
        }
        
        Column(
            horizontalAlignment = if (isFromUser) Alignment.End else Alignment.Start
        ) {
            // Message bubble
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isFromUser) 16.dp else 4.dp,
                            bottomEnd = if (isFromUser) 4.dp else 16.dp
                        )
                    )
                    .background(bubbleColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.content,
                    style = CustomTextStyles.MessageText,
                    color = textColor,
                    textAlign = if (isFromUser) TextAlign.End else TextAlign.Start
                )
            }
            
            // Timestamp
            Text(
                text = formattedTime,
                style = CustomTextStyles.MessageTimestamp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        
        // Add spacing for agent messages to push them to the left
        if (!isFromUser) {
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

/**
 * Simple timestamp formatting function for multiplatform compatibility.
 * Formats timestamp to HH:MM format.
 */
private fun formatTimestamp(timestamp: Long): String {
    val totalMinutes = (timestamp / (1000 * 60)) % (24 * 60)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val hoursStr = if (hours < 10) "0$hours" else "$hours"
    val minutesStr = if (minutes < 10) "0$minutes" else "$minutes"
    return "$hoursStr:$minutesStr"
}