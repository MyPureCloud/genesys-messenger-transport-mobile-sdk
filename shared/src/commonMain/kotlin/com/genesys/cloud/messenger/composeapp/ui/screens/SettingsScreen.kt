package com.genesys.cloud.messenger.composeapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.genesys.cloud.messenger.composeapp.model.AppSettings
import com.genesys.cloud.messenger.composeapp.model.ThemeMode
import com.genesys.cloud.messenger.composeapp.ui.components.SimpleTopBar
import com.genesys.cloud.messenger.composeapp.viewmodel.LanguageOption
import com.genesys.cloud.messenger.composeapp.viewmodel.SettingsUiState
import com.genesys.cloud.messenger.composeapp.viewmodel.SettingsViewModel

/**
 * Settings screen composable that displays app preferences and configuration options.
 * Allows users to modify theme, notifications, language, and other app settings.
 *
 * Requirements addressed:
 * - 2.1: Shared UI components using Compose Multiplatform
 * - 4.1: Basic navigation between screens
 * - 4.3: User interaction handling
 *
 * @param settingsViewModel ViewModel for managing settings state
 * @param onNavigateBack Callback when user wants to navigate back
 * @param modifier Optional modifier for the component
 */
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top bar
        SimpleTopBar(
            title = "Settings",
            showBackButton = true,
            onBackClick = onNavigateBack
        )
        
        // Settings content
        SettingsContent(
            uiState = uiState,
            settings = settings,
            onThemeChange = settingsViewModel::updateThemeMode,
            onNotificationsToggle = settingsViewModel::toggleNotifications,
            onLanguageChange = settingsViewModel::updateLanguage,
            onResetToDefaults = settingsViewModel::resetToDefaults,
            onClearError = settingsViewModel::clearError,
            onClearSuccessMessage = settingsViewModel::clearSuccessMessage,
            availableThemes = settingsViewModel.getAvailableThemeModes(),
            availableLanguages = settingsViewModel.getAvailableLanguages(),
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Main content of the settings screen
 */
@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    settings: AppSettings,
    onThemeChange: (ThemeMode) -> Unit,
    onNotificationsToggle: () -> Unit,
    onLanguageChange: (String) -> Unit,
    onResetToDefaults: () -> Unit,
    onClearError: () -> Unit,
    onClearSuccessMessage: () -> Unit,
    availableThemes: List<ThemeMode>,
    availableLanguages: List<LanguageOption>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Main settings content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Theme settings
            ThemeSettingsSection(
                currentTheme = settings.theme,
                availableThemes = availableThemes,
                onThemeChange = onThemeChange,
                enabled = !uiState.isLoading
            )
            
            // Notifications settings
            NotificationSettingsSection(
                notificationsEnabled = settings.notifications,
                onToggle = onNotificationsToggle,
                enabled = !uiState.isLoading
            )
            
            // Language settings
            LanguageSettingsSection(
                currentLanguage = settings.language,
                availableLanguages = availableLanguages,
                onLanguageChange = onLanguageChange,
                enabled = !uiState.isLoading
            )
            
            // Reset section
            ResetSettingsSection(
                onResetToDefaults = onResetToDefaults,
                enabled = !uiState.isLoading
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Loading overlay
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Updating settings...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        
        // Success message
        uiState.successMessage?.let { message ->
            SuccessMessage(
                message = message,
                onDismiss = onClearSuccessMessage,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        
        // Error message
        uiState.error?.let { error ->
            ErrorMessage(
                error = error,
                onDismiss = onClearError,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Theme settings section
 */
@Composable
private fun ThemeSettingsSection(
    currentTheme: ThemeMode,
    availableThemes: List<ThemeMode>,
    onThemeChange: (ThemeMode) -> Unit,
    enabled: Boolean
) {
    SettingsSection(
        title = "Appearance",
        description = "Choose your preferred theme"
    ) {
        Column(
            modifier = Modifier.selectableGroup()
        ) {
            availableThemes.forEach { theme ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = currentTheme == theme,
                            onClick = { if (enabled) onThemeChange(theme) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentTheme == theme,
                        onClick = null,
                        enabled = enabled
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = when (theme) {
                            ThemeMode.Light -> "Light"
                            ThemeMode.Dark -> "Dark"
                            ThemeMode.System -> "System default"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Notifications settings section
 */
@Composable
private fun NotificationSettingsSection(
    notificationsEnabled: Boolean,
    onToggle: () -> Unit,
    enabled: Boolean
) {
    SettingsSection(
        title = "Notifications",
        description = "Manage notification preferences"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Push notifications",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                )
                
                Text(
                    text = "Receive notifications for new messages",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = notificationsEnabled,
                onCheckedChange = { if (enabled) onToggle() },
                enabled = enabled
            )
        }
    }
}

/**
 * Language settings section
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSettingsSection(
    currentLanguage: String,
    availableLanguages: List<LanguageOption>,
    onLanguageChange: (String) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLanguageOption = availableLanguages.find { it.code == currentLanguage }
    
    SettingsSection(
        title = "Language",
        description = "Select your preferred language"
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) expanded = !expanded }
        ) {
            OutlinedTextField(
                value = currentLanguageOption?.displayName ?: "Unknown",
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableLanguages.forEach { language ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = language.displayName)
                                if (language.code == currentLanguage) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        onClick = {
                            onLanguageChange(language.code)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Reset settings section
 */
@Composable
private fun ResetSettingsSection(
    onResetToDefaults: () -> Unit,
    enabled: Boolean
) {
    SettingsSection(
        title = "Reset",
        description = "Restore default settings"
    ) {
        OutlinedButton(
            onClick = onResetToDefaults,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Reset to defaults")
        }
    }
}

/**
 * Reusable settings section container
 */
@Composable
private fun SettingsSection(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            
            content()
        }
    }
}

/**
 * Success message display
 */
@Composable
private fun SuccessMessage(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            
            TextButton(onClick = onDismiss) {
                Text(
                    text = "OK",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Error message display
 */
@Composable
private fun ErrorMessage(
    error: String,
    onDismiss: () -> Unit,
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
            modifier = Modifier.padding(16.dp)
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
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Dismiss",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}