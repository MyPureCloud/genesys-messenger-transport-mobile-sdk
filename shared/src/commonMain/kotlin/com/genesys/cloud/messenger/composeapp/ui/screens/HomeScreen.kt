package com.genesys.cloud.messenger.composeapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.genesys.cloud.messenger.composeapp.ui.components.SimpleTopBar
import com.genesys.cloud.messenger.composeapp.viewmodel.HomeViewModel
import com.genesys.cloud.messenger.composeapp.viewmodel.HomeUiState

/**
 * Home screen composable that displays the main landing page of the application.
 * Provides navigation to chat and settings screens.
 *
 * Requirements addressed:
 * - 2.1: Shared UI components using Compose Multiplatform
 * - 4.1: Basic navigation between screens
 * - 4.3: User interaction handling
 *
 * @param homeViewModel ViewModel for managing home screen state
 * @param onNavigateToChat Callback when user wants to navigate to chat
 * @param onNavigateToSettings Callback when user wants to navigate to settings
 * @param modifier Optional modifier for the component
 */
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by homeViewModel.uiState.collectAsState()
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top bar
        SimpleTopBar(
            title = "Messenger App",
            showBackButton = false
        )
        
        // Main content
        HomeContent(
            uiState = uiState,
            onNavigateToChat = {
                homeViewModel.navigateToChat()
                onNavigateToChat()
            },
            onNavigateToSettings = {
                homeViewModel.navigateToSettings()
                onNavigateToSettings()
            },
            onRetry = {
                homeViewModel.clearError()
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Main content of the home screen
 */
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onNavigateToChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Welcome section
            WelcomeSection()
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Navigation buttons
            NavigationButtons(
                onNavigateToChat = onNavigateToChat,
                onNavigateToSettings = onNavigateToSettings,
                enabled = !uiState.isLoading
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // App info section
            AppInfoSection()
        }
        
        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        // Error handling
        uiState.error?.let { error ->
            ErrorSection(
                error = error,
                onRetry = onRetry,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Welcome section with app title and description
 */
@Composable
private fun WelcomeSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to Messenger",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "A Compose Multiplatform messaging application template demonstrating shared UI components and ViewModels across Android and iOS platforms.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/**
 * Navigation buttons section
 */
@Composable
private fun NavigationButtons(
    onNavigateToChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    enabled: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Chat button
        ElevatedButton(
            onClick = onNavigateToChat,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            elevation = ButtonDefaults.elevatedButtonElevation(
                defaultElevation = 6.dp
            )
        ) {
            Text(
                text = "💬",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Start Chat",
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        // Settings button
        OutlinedButton(
            onClick = onNavigateToSettings,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * App information section
 */
@Composable
private fun AppInfoSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Features !",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val features = listOf(
                "Shared UI components across platforms",
                "Unified state management with ViewModels",
                "Material Design 3 theming",
                "Navigation between screens",
                "Cross-platform messaging interface"
            )
            
            features.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Error section for displaying errors
 */
@Composable
private fun ErrorSection(
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
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(onClick = onRetry) {
                Text(
                    text = "Retry",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}