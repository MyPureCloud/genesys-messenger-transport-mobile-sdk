package com.genesys.cloud.messenger.composeapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.genesys.cloud.messenger.composeapp.model.AppSettings
import com.genesys.cloud.messenger.composeapp.ui.components.SimpleTopBar
import com.genesys.cloud.messenger.composeapp.viewmodel.SettingsUiState
import com.genesys.cloud.messenger.composeapp.viewmodel.SettingsViewModel

/**
 * Simplified Settings screen composable for deployment configuration only.
 * Allows users to modify deploymentId and region settings.
 *
 * Requirements addressed:
 * - 1.1: Display only deploymentID and region configuration fields
 * - 1.2: Populate with default values from BuildConfig (AppConfig)
 * - 1.3: Validate and save deployment configuration
 * - 1.4: Remove appearance, notifications, and language settings
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
    val isLoading by settingsViewModel.isLoading.collectAsState()
    val error by settingsViewModel.error.collectAsState()
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top bar
        SimpleTopBar(
            title = "Deployment Settings",
            showBackButton = true,
            onBackClick = onNavigateBack
        )
        
        // Settings content
        DeploymentSettingsContent(
            uiState = uiState,
            settings = settings,
            isLoading = isLoading,
            error = error,
            onDeploymentIdChange = settingsViewModel::updateDeploymentId,
            onRegionChange = settingsViewModel::updateRegion,
            onResetToDefaults = settingsViewModel::resetToDefaults,
            onClearError = settingsViewModel::clearError,
            onClearSuccessMessage = settingsViewModel::clearSuccessMessage,
            onRetryLoad = settingsViewModel::reloadSettings,
            availableRegions = settingsViewModel.getAvailableRegions(),
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Main content of the deployment settings screen
 */
@Composable
private fun DeploymentSettingsContent(
    uiState: SettingsUiState,
    settings: AppSettings,
    isLoading: Boolean,
    error: com.genesys.cloud.messenger.composeapp.model.AppError?,
    onDeploymentIdChange: (String) -> Unit,
    onRegionChange: (String) -> Unit,
    onResetToDefaults: () -> Unit,
    onClearError: () -> Unit,
    onClearSuccessMessage: () -> Unit,
    onRetryLoad: () -> Unit,
    availableRegions: List<String>,
    modifier: Modifier = Modifier
) {
    var deploymentIdText by remember(settings.deploymentId) { mutableStateOf(settings.deploymentId) }
    
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
            // Deployment ID settings
            DeploymentIdSection(
                deploymentId = deploymentIdText,
                onDeploymentIdChange = { newValue ->
                    deploymentIdText = newValue
                    onDeploymentIdChange(newValue)
                },
                enabled = !isLoading
            )
            
            // Region settings
            RegionSettingsSection(
                currentRegion = settings.region,
                availableRegions = availableRegions,
                onRegionChange = onRegionChange,
                enabled = !isLoading
            )
            
            // Reset section
            ResetSettingsSection(
                onResetToDefaults = {
                    onResetToDefaults()
                    // Reset local text field state when defaults are restored
                },
                enabled = !isLoading
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Loading overlay
        if (isLoading) {
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
        
        // Error message using new error components
        error?.let { appError ->
            com.genesys.cloud.messenger.composeapp.ui.components.ErrorSnackbar(
                error = appError,
                onDismiss = onClearError,
                onRetry = onRetryLoad,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Deployment ID settings section
 */
@Composable
private fun DeploymentIdSection(
    deploymentId: String,
    onDeploymentIdChange: (String) -> Unit,
    enabled: Boolean
) {
    SettingsSection(
        title = "Deployment ID",
        description = "Enter your Genesys Cloud deployment ID"
    ) {
        OutlinedTextField(
            value = deploymentId,
            onValueChange = onDeploymentIdChange,
            enabled = enabled,
            placeholder = { Text("e.g., 12345678-1234-1234-1234-123456789abc") },
            supportingText = { Text("UUID format required") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

/**
 * Region settings section
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegionSettingsSection(
    currentRegion: String,
    availableRegions: List<String>,
    onRegionChange: (String) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    
    SettingsSection(
        title = "Region",
        description = "Select your Genesys Cloud region"
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) expanded = !expanded }
        ) {
            OutlinedTextField(
                value = currentRegion,
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
                availableRegions.forEach { region ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = region)
                                if (region == currentRegion) {
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
                            onRegionChange(region)
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
        description = "Restore default deployment settings"
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

