package com.genesys.cloud.messenger.composeapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.genesys.cloud.messenger.composeapp.theme.CustomTextStyles

/**
 * A composable top bar component for the application with customizable title and actions.
 *
 * @param title The title text to display in the top bar
 * @param modifier Optional modifier for the component
 * @param navigationIcon Optional navigation icon (typically back arrow)
 * @param onNavigationClick Callback when navigation icon is clicked
 * @param actions Optional list of action items to display on the right
 * @param backgroundColor Background color of the top bar
 * @param contentColor Color of the content (text and icons)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    backgroundColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = CustomTextStyles.AppBarTitle,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier,
        navigationIcon = {
            if (navigationIcon != null && onNavigationClick != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = "Navigate back",
                        tint = contentColor
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor,
            titleContentColor = contentColor,
            navigationIconContentColor = contentColor,
            actionIconContentColor = contentColor
        )
    )
}

/**
 * A specialized top bar for chat screens with status indicator.
 *
 * @param title The chat title (e.g., agent name or "Chat")
 * @param subtitle Optional subtitle (e.g., "Online", "Typing...")
 * @param modifier Optional modifier for the component
 * @param onBackClick Callback when back button is clicked
 * @param onMenuClick Callback when menu button is clicked
 */
@Composable
fun ChatTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null
) {
    TopBar(
        title = title,
        modifier = modifier,
        navigationIcon = if (onBackClick != null) Icons.AutoMirrored.Filled.ArrowBack else null,
        onNavigationClick = onBackClick,
        actions = {
            // Chat title and subtitle in a column
            if (subtitle != null) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = title,
                        style = CustomTextStyles.AppBarTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Menu button
            if (onMenuClick != null) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    )
}

/**
 * A simple top bar for basic screens.
 *
 * @param title The screen title
 * @param modifier Optional modifier for the component
 * @param showBackButton Whether to show the back navigation button
 * @param onBackClick Callback when back button is clicked
 */
@Composable
fun SimpleTopBar(
    title: String,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    onBackClick: (() -> Unit)? = null
) {
    TopBar(
        title = title,
        modifier = modifier,
        navigationIcon = if (showBackButton) Icons.AutoMirrored.Filled.ArrowBack else null,
        onNavigationClick = onBackClick
    )
}