package com.genesys.cloud.messenger.composeapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.genesys.cloud.messenger.composeapp.model.Command
import com.genesys.cloud.messenger.transport.core.MessagingClient
import com.genesys.cloud.messenger.composeapp.model.SocketMessage
import com.genesys.cloud.messenger.composeapp.platform.PlatformContextProvider
import com.genesys.cloud.messenger.composeapp.viewmodel.TestBedViewModel
import com.genesys.cloud.messenger.composeapp.viewmodel.SettingsViewModel
import com.genesys.cloud.messenger.composeapp.util.formatTimestamp
import com.genesys.cloud.messenger.composeapp.util.getCurrentTimeMillis

/**
 * Interaction screen composable that displays the TestBed interface.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractionScreen(
    testBedViewModel: TestBedViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Collect state from ViewModels
    val socketMessages by testBedViewModel.socketMessages.collectAsState()
    val availableCommands by testBedViewModel.availableCommands.collectAsState()
    val clientState = testBedViewModel.clientState
    val commandWaiting = testBedViewModel.commandWaiting
    val currentCommand = testBedViewModel.command
    val settings by settingsViewModel.settings.collectAsState()
    val lastError by testBedViewModel.lastError.collectAsState()
    
    // Initialize TestBedViewModel when screen loads and settings are available
    LaunchedEffect(settings.deploymentId, settings.region) {
        println("InteractionScreen: LaunchedEffect triggered - deploymentId: ${settings.deploymentId}, region: ${settings.region}")
        println("InteractionScreen: Platform context available: ${PlatformContextProvider.isPlatformContextAvailable()}")
        
        if (settings.deploymentId.isNotEmpty() && 
            settings.region.isNotEmpty() && 
            PlatformContextProvider.isPlatformContextAvailable()) {
            
            println("InteractionScreen: All conditions met for initialization")
            
            if (!testBedViewModel.isInitialized ||
                testBedViewModel.deploymentId != settings.deploymentId ||
                testBedViewModel.region != settings.region) {
                
                println("InteractionScreen: Starting TestBedViewModel initialization")
                testBedViewModel.deploymentId = settings.deploymentId
                testBedViewModel.region = settings.region
                
                val platformContext = PlatformContextProvider.getCurrentPlatformContext()
                if (platformContext != null) {
                    println("InteractionScreen: Platform context obtained, calling init()")
                    testBedViewModel.init(
                        platformContext = platformContext,
                        selectFile = { fileProfile ->
                            println("File selection requested for: ${fileProfile}")
                        },
                        onOktaSignIn = { url ->
                            println("OAuth sign-in requested for URL: $url")
                        }
                    )
                } else {
                    println("InteractionScreen: Platform context is null despite being available")
                }
            } else {
                println("InteractionScreen: TestBedViewModel already initialized with current settings")
            }
        } else {
            println("InteractionScreen: Initialization conditions not met:")
            println("  - deploymentId empty: ${settings.deploymentId.isEmpty()}")
            println("  - region empty: ${settings.region.isEmpty()}")
            println("  - platform context unavailable: ${!PlatformContextProvider.isPlatformContextAvailable()}")
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top bar
        TopAppBar(
            title = { Text("TestBed Interaction") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        
        // Client state indicator
        ClientStateIndicator(
            clientState = clientState,
            isInitialized = testBedViewModel.isInitialized,
            deploymentId = settings.deploymentId,
            region = settings.region,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Socket messages display
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(socketMessages) { message ->
                SocketMessageCard(
                    message = message,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Command input interface
        CommandInputInterface(
            availableCommands = availableCommands,
            currentCommand = currentCommand,
            commandWaiting = commandWaiting,
            onCommandChanged = { command -> testBedViewModel.onCommandChanged(command) },
            onCommandSend = { testBedViewModel.onCommandSend() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ClientStateIndicator(
    clientState: MessagingClient.State,
    isInitialized: Boolean,
    deploymentId: String,
    region: String,
    modifier: Modifier = Modifier
) {
    val stateText = when (clientState) {
        is MessagingClient.State.Idle -> "Idle"
        is MessagingClient.State.Connecting -> "Connecting..."
        is MessagingClient.State.Connected -> "Connected"
        is MessagingClient.State.Reconnecting -> "Reconnecting..."
        is MessagingClient.State.Configured -> "Configured"
        is MessagingClient.State.ReadOnly -> "Read Only"
        is MessagingClient.State.Closing -> "Closing..."
        is MessagingClient.State.Closed -> "Closed"
        is MessagingClient.State.Error -> "Error"
        else -> "Unknown"
    }
    
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stateText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                if (deploymentId.isNotEmpty() && region.isNotEmpty()) {
                    Text(
                        text = "$deploymentId • $region",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Text(
                text = if (isInitialized) "✅" else "⚪",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun SocketMessageCard(
    message: SocketMessage,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = message.type,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommandInputInterface(
    availableCommands: List<Command>,
    currentCommand: String,
    commandWaiting: Boolean,
    onCommandChanged: (String) -> Unit,
    onCommandSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Command dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded && !commandWaiting },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = currentCommand,
                    onValueChange = onCommandChanged,
                    label = { Text("Command") },
                    trailingIcon = { 
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) 
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    enabled = !commandWaiting,
                    singleLine = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableCommands.forEach { command ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = command.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = command.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onCommandChanged(command.name)
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Send button
            Button(
                onClick = onCommandSend,
                enabled = !commandWaiting && currentCommand.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (commandWaiting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Executing...")
                } else {
                    Text("Execute")
                }
            }
        }
    }
}