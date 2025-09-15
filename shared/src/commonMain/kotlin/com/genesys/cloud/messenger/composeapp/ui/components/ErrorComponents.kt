package com.genesys.cloud.messenger.composeapp.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.genesys.cloud.messenger.composeapp.model.AppError
import kotlinx.coroutines.delay

/**
 * Comprehensive error display component that shows different types of errors
 * with appropriate styling and actions.
 * 
 * This component provides a unified way to display errors across the application
 * with consistent styling and behavior. It supports different error types with
 * appropriate icons, colors, and actions.
 * 
 * Features:
 * - Type-safe error handling with AppError sealed class
 * - Consistent Material Design 3 styling
 * - Configurable actions (retry, dismiss)
 * - Accessibility support
 * - Cross-platform compatibility
 * 
 * Performance Considerations:
 * - Uses stable composable structure to minimize recomposition
 * - Efficient color and icon resolution
 * - Optimized for different screen sizes
 * 
 * @param error The error to display (typed with AppError sealed class)
 * @param onDismiss Optional callback when user dismisses the error
 * @param onRetry Optional callback when user wants to retry the failed operation
 * @param modifier Modifier for customizing the component's appearance
 * @param showIcon Whether to show the error icon (default: true)
 * @param dismissible Whether the error can be dismissed (default: true)
 */
@Composable
fun ErrorDisplay(
    error: AppError,
    onDismiss: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    dismissible: Boolean = true
) {
    val errorInfo = getErrorDisplayInfo(error)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = errorInfo.backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Error icon
            if (showIcon) {
                Icon(
                    imageVector = errorInfo.icon,
                    contentDescription = null,
                    tint = errorInfo.iconColor,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            // Error content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = errorInfo.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = errorInfo.textColor
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = error.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = errorInfo.textColor.copy(alpha = 0.9f)
                )
                
                // Action buttons
                if (onRetry != null || (onDismiss != null && dismissible)) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onRetry != null) {
                            TextButton(
                                onClick = onRetry,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = errorInfo.textColor
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retry")
                            }
                        }
                        
                        if (onDismiss != null && dismissible) {
                            TextButton(
                                onClick = onDismiss,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = errorInfo.textColor
                                )
                            ) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
            
            // Close button
            if (onDismiss != null && dismissible) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = errorInfo.textColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Inline error display for form fields
 */
@Composable
fun InlineErrorDisplay(
    error: AppError?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = error != null,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        error?.let { err ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(6.dp))
                
                Text(
                    text = err.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Snackbar-style error display that appears at the bottom
 */
@Composable
fun ErrorSnackbar(
    error: AppError,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    autoDismissDelay: Long = 5000L
) {
    LaunchedEffect(error) {
        if (autoDismissDelay > 0) {
            delay(autoDismissDelay)
            onDismiss()
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = error.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            
            if (onRetry != null) {
                TextButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Retry")
                }
            }
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Full-screen error display for critical errors
 */
@Composable
fun FullScreenErrorDisplay(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val errorInfo = getErrorDisplayInfo(error)
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = errorInfo.icon,
            contentDescription = null,
            tint = errorInfo.iconColor,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = errorInfo.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = error.message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onRetry != null) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Try Again")
                }
            }
            
            if (onDismiss != null) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        }
    }
}

/**
 * Loading state with error fallback
 */
@Composable
fun LoadingWithError(
    isLoading: Boolean,
    error: AppError?,
    onRetry: (() -> Unit)? = null,
    onDismissError: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        error?.let { err ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ErrorDisplay(
                    error = err,
                    onRetry = onRetry,
                    onDismiss = onDismissError,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

// Helper data class and function for error display information
private data class ErrorDisplayInfo(
    val title: String,
    val icon: ImageVector,
    val iconColor: androidx.compose.ui.graphics.Color,
    val backgroundColor: androidx.compose.ui.graphics.Color,
    val textColor: androidx.compose.ui.graphics.Color
)

@Composable
private fun getErrorDisplayInfo(error: AppError): ErrorDisplayInfo {
    return when (error) {
        is AppError.NetworkError -> ErrorDisplayInfo(
            title = "Connection Error",
            icon = Icons.Default.Warning,
            iconColor = MaterialTheme.colorScheme.onErrorContainer,
            backgroundColor = MaterialTheme.colorScheme.errorContainer,
            textColor = MaterialTheme.colorScheme.onErrorContainer
        )
        is AppError.ValidationError -> ErrorDisplayInfo(
            title = "Invalid Input",
            icon = Icons.Default.Warning,
            iconColor = MaterialTheme.colorScheme.onErrorContainer,
            backgroundColor = MaterialTheme.colorScheme.errorContainer,
            textColor = MaterialTheme.colorScheme.onErrorContainer
        )
        is AppError.PlatformError -> ErrorDisplayInfo(
            title = "System Error",
            icon = Icons.Default.Warning,
            iconColor = MaterialTheme.colorScheme.onErrorContainer,
            backgroundColor = MaterialTheme.colorScheme.errorContainer,
            textColor = MaterialTheme.colorScheme.onErrorContainer
        )
        is AppError.BusinessError -> ErrorDisplayInfo(
            title = "Operation Failed",
            icon = Icons.Default.Warning,
            iconColor = MaterialTheme.colorScheme.onErrorContainer,
            backgroundColor = MaterialTheme.colorScheme.errorContainer,
            textColor = MaterialTheme.colorScheme.onErrorContainer
        )
        is AppError.UnknownError -> ErrorDisplayInfo(
            title = "Unexpected Error",
            icon = Icons.Default.Info,
            iconColor = MaterialTheme.colorScheme.onErrorContainer,
            backgroundColor = MaterialTheme.colorScheme.errorContainer,
            textColor = MaterialTheme.colorScheme.onErrorContainer
        )
        else -> ErrorDisplayInfo(
            title = "Error",
            icon = Icons.Default.Warning,
            iconColor = MaterialTheme.colorScheme.onErrorContainer,
            backgroundColor = MaterialTheme.colorScheme.errorContainer,
            textColor = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}