# Design Document

## Overview

This design outlines the refactoring of the Compose Multiplatform template to create a developer-focused testbed tool. The design transforms the existing chat application into an interaction monitoring and command execution interface, implementing business logic in the shared module based on the Android TestBedViewModel reference.

## Architecture

### High-Level Architecture

```
┌─────────────────┐    ┌─────────────────┐
│   composeApp    │    │  iosComposeApp  │
│   (Android)     │    │     (iOS)       │
└─────────┬───────┘    └─────────┬───────┘
          │                      │
          └──────────┬───────────┘
                     │
          ┌─────────────────────┐
          │   Shared Module     │
          │                     │
          │ ┌─────────────────┐ │
          │ │ TestBedViewModel│ │
          │ │                 │ │
          │ │ - Commands      │ │
          │ │ - Socket Msgs   │ │
          │ │ - Auth State    │ │
          │ └─────────────────┘ │
          │                     │
          │ ┌─────────────────┐ │
          │ │SettingsViewModel│ │
          │ │                 │ │
          │ │ - DeploymentID  │ │
          │ │ - Region        │ │
          │ └─────────────────┘ │
          │                     │
          │ ┌─────────────────┐ │
          │ │   UI Screens    │ │
          │ │                 │ │
          │ │ - Settings      │ │
          │ │ - Interaction   │ │
          │ └─────────────────┘ │
          └─────────────────────┘
```

### Component Responsibilities

1. **TestBedViewModel (Shared)**: Core business logic for messaging client operations, command execution, and socket message handling
2. **SettingsViewModel (Shared)**: Simplified configuration management for deploymentID and region only
3. **InteractionScreen (Shared)**: Developer interface for command execution and socket message monitoring
4. **SettingsScreen (Shared)**: Simplified settings interface for deployment configuration

## Components and Interfaces

### TestBedViewModel

Based on the Android TestBedViewModel reference, the shared implementation will include:

```kotlin
class TestBedViewModel : BaseViewModel() {
    // State properties
    var command: String by mutableStateOf("")
    var commandWaiting: Boolean by mutableStateOf(false)
    var socketMessage: String by mutableStateOf("")
    var clientState: MessagingClient.State by mutableStateOf(State.Idle)
    var deploymentId: String by mutableStateOf("")
    var region: String by mutableStateOf("")
    var authState: AuthState by mutableStateOf(AuthState.NoAuth)
    
    // Transport SDK components
    private lateinit var messengerTransport: MessengerTransportSDK
    private lateinit var client: MessagingClient
    private lateinit var pushService: PushService
    
    // Initialization state
    var isInitialized: Boolean by mutableStateOf(false)
    
    // Available commands list
    val availableCommands: List<Command>
    
    // Initialization
    fun init(
        platformContext: PlatformContext,
        selectFile: (fileAttachmentProfile: FileAttachmentProfile) -> Unit,
        onOktaSignIn: (url: String) -> Unit
    )
    
    // Update deployment settings and reinitialize if needed
    fun updateDeploymentSettings(deploymentId: String, region: String)
    
    // Core functionality
    fun onCommandSend()
    fun onCommandChanged(newCommand: String)
    fun onDeploymentIdChanged(newDeploymentId: String)
    fun onRegionChanged(newRegion: String)
    
    // Command implementations
    private fun doConnect()
    private fun doConnectAuthenticated()
    private fun doDisconnect()
    private fun doSendMessage(message: String)
    // ... all other commands from reference
}
```

### Command System

```kotlin
data class Command(
    val name: String,
    val description: String,
    val requiresInput: Boolean = false,
    val inputPlaceholder: String? = null
)

sealed class AuthState {
    object NoAuth : AuthState()
    data class AuthCodeReceived(val authCode: String) : AuthState()
    object Authorized : AuthState()
    object LoggedOut : AuthState()
    data class Error(
        val errorCode: ErrorCode,
        val message: String?,
        val correctiveAction: CorrectiveAction
    ) : AuthState()
}
```

### SettingsViewModel (Simplified)

```kotlin
class SettingsViewModel : BaseViewModel() {
    private val _deploymentId = MutableStateFlow("")
    val deploymentId: StateFlow<String> = _deploymentId.asStateFlow()
    
    private val _region = MutableStateFlow("")
    val region: StateFlow<String> = _region.asStateFlow()
    
    val availableRegions: List<String>
    
    fun updateDeploymentId(id: String)
    fun updateRegion(region: String)
    fun loadDefaultValues()
}
```

### InteractionScreen

```kotlin
@Composable
fun InteractionScreen(
    testBedViewModel: TestBedViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Command input with dropdown
    // Socket message display with expandable details
    // Client state indicator
    // Loading states
}
```

### Socket Message Display

```kotlin
data class SocketMessageItem(
    val id: String,
    val timestamp: Long,
    val messageType: String,
    val summary: String,
    val fullContent: String,
    val isExpanded: Boolean = false
)

@Composable
fun SocketMessageList(
    messages: List<SocketMessageItem>,
    onToggleExpand: (String) -> Unit,
    modifier: Modifier = Modifier
)
```

## Data Models

### Core Models

```kotlin
// Simplified settings model
data class TestBedSettings(
    val deploymentId: String = "",
    val region: String = ""
)

// Command execution state
data class CommandExecutionState(
    val isExecuting: Boolean = false,
    val currentCommand: String? = null,
    val result: String? = null,
    val error: String? = null
)

// Socket message model
data class SocketMessage(
    val id: String,
    val timestamp: Long,
    val type: String,
    val content: String,
    val rawMessage: String
)
```

### State Management

```kotlin
data class TestBedUiState(
    val socketMessages: List<SocketMessage> = emptyList(),
    val commandExecutionState: CommandExecutionState = CommandExecutionState(),
    val clientConnectionState: String = "Idle",
    val expandedMessageIds: Set<String> = emptySet()
)

data class SettingsUiState(
    val isLoading: Boolean = false,
    val saveSuccess: Boolean = false
)
```

## Error Handling

### Error Types

```kotlin
sealed class TestBedError : AppError {
    data class CommandExecutionError(
        override val message: String,
        val command: String,
        override val cause: Throwable? = null
    ) : TestBedError()
    
    data class ConnectionError(
        override val message: String,
        override val cause: Throwable? = null
    ) : TestBedError()
    
    data class AuthenticationError(
        override val message: String,
        val authState: AuthState,
        override val cause: Throwable? = null
    ) : TestBedError()
}
```

### Error Handling Strategy

1. **Command Errors**: Display in socket message area with error formatting
2. **Connection Errors**: Show in UI with retry options
3. **Authentication Errors**: Handle through auth state management
4. **Validation Errors**: Inline validation for deployment settings

## Testing Strategy

### Unit Tests

```kotlin
class TestBedViewModelTest {
    @Test
    fun `command execution updates state correctly`()
    
    @Test
    fun `socket message processing works`()
    
    @Test
    fun `authentication flow handles states`()
    
    @Test
    fun `deployment configuration validation`()
}

class SettingsViewModelTest {
    @Test
    fun `deployment settings update correctly`()
    
    @Test
    fun `default values load from BuildConfig`()
    
    @Test
    fun `region validation works`()
}
```

### Integration Tests

```kotlin
class InteractionScreenTest {
    @Test
    fun `command dropdown shows all available commands`()
    
    @Test
    fun `socket messages display and expand correctly`()
    
    @Test
    fun `command execution shows loading states`()
}

class SettingsScreenTest {
    @Test
    fun `settings screen shows only deployment fields`()
    
    @Test
    fun `settings save and load correctly`()
}
```

## Initialization Flow

### TestBedViewModel Initialization

The TestBedViewModel initialization follows this sequence:

1. **ViewModel Creation**: TestBedViewModel is instantiated by the DI system
2. **Settings Loading**: Load deploymentId and region from SettingsViewModel or BuildConfig defaults
3. **Platform Context Setup**: Receive platform-specific context (Android Context or iOS equivalent)
4. **Transport SDK Initialization**: 
   - Create Configuration object with deploymentId, region, logging, and vault settings
   - Initialize MessengerTransportSDK with configuration
   - Create MessagingClient from transport SDK
   - Create PushService from transport SDK
5. **Client Configuration**:
   - Set up client event listeners (stateChangedListener, messageListener, eventListener)
   - Add custom attributes to client (SDK version, etc.)
   - Configure client state tracking
   - Set initial client state to current state
6. **Asset Loading**: Load saved attachments and prepare file handling
7. **Deployment Config**: Fetch deployment configuration from transport SDK
8. **Ready State**: ViewModel is ready for command execution

### Client Initialization Details

```kotlin
fun init(
    platformContext: PlatformContext,
    selectFile: (fileAttachmentProfile: FileAttachmentProfile) -> Unit,
    onOktaSignIn: (url: String) -> Unit
) {
    // Step 1: Configure vault based on settings
    val configuration = Configuration(
        deploymentId = deploymentId.ifEmpty { getDefaultDeploymentId() },
        domain = region.ifEmpty { getDefaultRegion() },
        logging = true,
        encryptedVault = true
    )
    
    // Step 2: Initialize vault context
    if (configuration.encryptedVault) {
        EncryptedVault.context = platformContext.getContext()
    } else {
        DefaultVault.context = platformContext.getContext()
    }
    
    // Step 3: Create transport SDK and clients
    messengerTransport = MessengerTransportSDK(configuration)
    client = messengerTransport.createMessagingClient()
    pushService = messengerTransport.createPushService()
    
    // Step 4: Configure client listeners and state
    with(client) {
        stateChangedListener = { onClientStateChanged(it.oldState, it.newState) }
        messageListener = { onMessage(it) }
        eventListener = { onEvent(it) }
        
        // Add custom attributes
        customAttributesStore.add(mapOf(
            "sdkVersion" to "Transport SDK: ${MessengerTransportSDK.sdkVersion}"
        ))
        
        // Set initial state
        clientState = currentState
    }
    
    // Step 5: Load assets and fetch deployment config
    loadSavedAttachments(platformContext)
    
    scope.launch {
        try {
            messengerTransport.fetchDeploymentConfig()
        } catch (e: Exception) {
            onSocketMessageReceived("Failed to fetch deployment config: ${e.message}")
        }
    }
}
```

### Initialization Trigger Points

```kotlin
// In InteractionScreen composable
@Composable
fun InteractionScreen(
    testBedViewModel: TestBedViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by settingsViewModel.settings.collectAsState()
    val clientState by testBedViewModel.clientState.collectAsState()
    
    // Initialize TestBedViewModel when screen loads and settings are available
    LaunchedEffect(settings.deploymentId, settings.region) {
        if (settings.deploymentId.isNotEmpty() && settings.region.isNotEmpty()) {
            // Only initialize if not already initialized
            if (clientState == MessagingClient.State.Idle && !testBedViewModel.isInitialized) {
                testBedViewModel.init(
                    platformContext = getPlatformContext(),
                    selectFile = { fileProfile -> 
                        // Handle file selection for attachments
                        handleFileSelection(fileProfile)
                    },
                    onOktaSignIn = { url -> 
                        // Handle OAuth sign-in flow
                        handleOktaSignIn(url)
                    }
                )
            }
        }
    }
    
    // Re-initialize when deployment settings change
    LaunchedEffect(settings.deploymentId, settings.region) {
        if (testBedViewModel.isInitialized && 
            (testBedViewModel.deploymentId != settings.deploymentId || 
             testBedViewModel.region != settings.region)) {
            
            // Update deployment settings and reinitialize if needed
            testBedViewModel.updateDeploymentSettings(
                deploymentId = settings.deploymentId,
                region = settings.region
            )
        }
    }
    
    // Rest of UI...
}
```

### Platform-Specific Context

```kotlin
expect class PlatformContext

// Android implementation
actual class PlatformContext(val context: Context)

// iOS implementation  
actual class PlatformContext(/* iOS equivalent */)

expect fun getPlatformContext(): PlatformContext
```

## Implementation Approach

### Phase 1: Core Infrastructure
1. Create simplified SettingsViewModel with deployment configuration only
2. Implement TestBedViewModel with initialization flow and command system infrastructure
3. Create basic InteractionScreen layout with initialization handling

### Phase 2: Command System
1. Implement all commands from Android TestBedViewModel reference
2. Add command dropdown and input handling
3. Implement socket message processing and display

### Phase 3: UI Polish
1. Add expandable socket message display
2. Implement loading states and error handling
3. Add client state indicators

### Phase 4: Cleanup
1. Remove unused code from current implementation
2. Update navigation to use InteractionScreen
3. Clean up dependencies and imports

## Platform-Specific Considerations

### Android Platform
- Use existing transport SDK integration patterns
- Maintain compatibility with current build configuration
- Leverage Android-specific file handling for attachments

### iOS Platform
- Implement equivalent transport SDK integration
- Handle iOS-specific authentication flows
- Adapt file attachment handling for iOS

### Shared Logic
- All business logic in shared ViewModels
- Platform-agnostic command execution
- Shared state management and error handling