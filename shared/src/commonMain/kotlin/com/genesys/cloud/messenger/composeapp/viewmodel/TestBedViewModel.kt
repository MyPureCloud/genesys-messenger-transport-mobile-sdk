package com.genesys.cloud.messenger.composeapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.genesys.cloud.messenger.composeapp.model.AuthState
import com.genesys.cloud.messenger.composeapp.model.Command
import com.genesys.cloud.messenger.composeapp.model.SocketMessage
import com.genesys.cloud.messenger.composeapp.model.ValidationResult
import com.genesys.cloud.messenger.composeapp.model.AttachmentState
import com.genesys.cloud.messenger.composeapp.model.SavedAttachment
import com.genesys.cloud.messenger.composeapp.model.TestBedError
import com.genesys.cloud.messenger.composeapp.model.PlatformContext
import com.genesys.cloud.messenger.composeapp.model.toTestBedError
import com.genesys.cloud.messenger.transport.core.MessengerTransportSDK
import com.genesys.cloud.messenger.transport.core.MessagingClient
import com.genesys.cloud.messenger.transport.core.Configuration
import com.genesys.cloud.messenger.transport.core.FileAttachmentProfile
import com.genesys.cloud.messenger.transport.core.Attachment
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.StateChange
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.MessageEvent
import com.genesys.cloud.messenger.transport.push.PushService
import com.genesys.cloud.messenger.composeapp.util.getCurrentTimeMillis
import com.genesys.cloud.messenger.composeapp.util.formatTimestamp
import com.genesys.cloud.messenger.composeapp.util.ErrorHandler
import com.genesys.cloud.messenger.composeapp.model.RetryConfig
import com.genesys.cloud.messenger.composeapp.model.ErrorRecoveryAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * TestBedViewModel for managing messaging client operations and command execution.
 */
class TestBedViewModel : BaseViewModel() {
    
    // Core state properties
    var command: String by mutableStateOf("")
    var commandWaiting: Boolean by mutableStateOf(false)
    var socketMessage: String by mutableStateOf("")
    var clientState: MessagingClient.State by mutableStateOf(MessagingClient.State.Idle)
    var deploymentId: String by mutableStateOf("")
    var region: String by mutableStateOf("")
    var authState: AuthState by mutableStateOf(AuthState.NoAuth)
    var isInitialized: Boolean by mutableStateOf(false)
    
    // Transport SDK components
    private var messengerTransport: MessengerTransportSDK? = null
    private var client: MessagingClient? = null
    private var pushService: PushService? = null
    
    // Available commands list
    private val _availableCommands = MutableStateFlow(createAvailableCommands())
    val availableCommands: StateFlow<List<Command>> = _availableCommands.asStateFlow()
    
    // Socket messages list for display
    private val _socketMessages = MutableStateFlow<List<SocketMessage>>(emptyList())
    val socketMessages: StateFlow<List<SocketMessage>> = _socketMessages.asStateFlow()
    
    // Callback functions for platform-specific operations
    private var selectFileCallback: ((fileAttachmentProfile: FileAttachmentProfile) -> Unit)? = null
    private var oktaSignInCallback: ((url: String) -> Unit)? = null
    
    // Attachment state management
    private val _attachments = MutableStateFlow<List<Attachment>>(emptyList())
    val attachments: StateFlow<List<Attachment>> = _attachments.asStateFlow()
    
    private val _savedAttachments = MutableStateFlow<List<SavedAttachment>>(emptyList())
    val savedAttachments: StateFlow<List<SavedAttachment>> = _savedAttachments.asStateFlow()
    
    private var fileAttachmentProfile: FileAttachmentProfile? = null
    
    // Error handling
    private val errorHandler = ErrorHandler()
    val lastError: StateFlow<TestBedError?> = errorHandler.lastError
    val errorHistory: StateFlow<List<TestBedError>> = errorHandler.errorHistory
    
    /**
     * Initialize the TestBedViewModel with platform context and callbacks.
     */
    fun init(
        platformContext: PlatformContext,
        selectFile: (fileAttachmentProfile: FileAttachmentProfile) -> Unit,
        onOktaSignIn: (url: String) -> Unit
    ) {
        if (isInitialized) {
            addSocketMessageInternal(
                SocketMessage(
                    id = generateMessageId(),
                    timestamp = getCurrentTimeMillis(),
                    type = "System",
                    content = "Already initialized",
                    rawMessage = "TestBedViewModel is already initialized"
                )
            )
            return
        }
        
        selectFileCallback = selectFile
        oktaSignInCallback = onOktaSignIn
        
        addSocketMessageInternal(
            SocketMessage(
                id = generateMessageId(),
                timestamp = getCurrentTimeMillis(),
                type = "System",
                content = "Starting initialization...",
                rawMessage = "Initializing TestBedViewModel with deploymentId: $deploymentId, region: $region"
            )
        )
        
        try {
            val configuration = createConfiguration()
            initializeVaultContext(platformContext, configuration)
            initializeTransportSDK(configuration)
            configureClientListeners()
            loadSavedAttachments(platformContext)
            fetchDeploymentConfig()
            
            isInitialized = true
            
            addSocketMessageInternal(
                SocketMessage(
                    id = generateMessageId(),
                    timestamp = getCurrentTimeMillis(),
                    type = "System",
                    content = "Initialization completed successfully",
                    rawMessage = "TestBedViewModel fully initialized and ready for commands"
                )
            )
            
        } catch (e: Exception) {
            val error = TestBedError.ConnectionError.InitializationError(
                reason = e.message ?: "Unknown initialization error",
                cause = e
            )
            
            handleError(error)
            isInitialized = false
            clientState = MessagingClient.State.Error(ErrorCode.UnexpectedError, e.message)
        }
    }
    
    /**
     * Create configuration object for transport SDK
     */
    private fun createConfiguration(): Configuration {
        return Configuration(
            deploymentId = deploymentId.ifEmpty { "b0e4c5ab-12c1-4a5f-8e9d-3f2a1b4c5d6e" },
            domain = region.ifEmpty { "mypurecloud.com" },
            logging = true,
            encryptedVault = true
        )
    }
    
    /**
     * Initialize vault context based on configuration
     */
    private fun initializeVaultContext(platformContext: PlatformContext, configuration: Configuration) {
        addSocketMessageInternal(
            SocketMessage(
                id = generateMessageId(),
                timestamp = getCurrentTimeMillis(),
                type = "System",
                content = "Setting up vault context (encrypted: ${configuration.encryptedVault})",
                rawMessage = "Vault context setup: useEncryptedVault=${configuration.encryptedVault}"
            )
        )
        
        try {
            platformContext.setupVaultContext(configuration.encryptedVault)
            
            addSocketMessageInternal(
                SocketMessage(
                    id = generateMessageId(),
                    timestamp = getCurrentTimeMillis(),
                    type = "System",
                    content = "Vault context setup completed successfully",
                    rawMessage = "Vault context initialized"
                )
            )
        } catch (e: Exception) {
            addSocketMessageInternal(
                SocketMessage(
                    id = generateMessageId(),
                    timestamp = getCurrentTimeMillis(),
                    type = "Error",
                    content = "Failed to setup vault context: ${e.message}",
                    rawMessage = "Vault context error: ${e.message}"
                )
            )
            throw e
        }
    }
    
    /**
     * Initialize transport SDK and create clients
     */
    private fun initializeTransportSDK(configuration: Configuration) {
        messengerTransport = MessengerTransportSDK(configuration)
        client = messengerTransport?.createMessagingClient()
        pushService = messengerTransport?.createPushService()
    }
    
    /**
     * Configure client listeners and state
     */
    private fun configureClientListeners() {
        client?.stateChangedListener = { stateChange ->
            clientState = stateChange.newState
            addSocketMessageInternal(
                SocketMessage(
                    id = generateMessageId(),
                    timestamp = getCurrentTimeMillis(),
                    type = "StateChange",
                    content = "State changed to: ${stateChange.newState}",
                    rawMessage = "StateChange: ${stateChange.oldState} -> ${stateChange.newState}"
                )
            )
        }
        
        client?.messageListener = { messageEvent ->
            val message = when (messageEvent) {
                is MessageEvent.MessageInserted -> messageEvent.message
                is MessageEvent.MessageUpdated -> messageEvent.message
                is MessageEvent.QuickReplyReceived -> messageEvent.message
                is MessageEvent.AttachmentUpdated -> null
                is MessageEvent.HistoryFetched -> null
            }
            
            if (message != null) {
                addSocketMessageInternal(
                    SocketMessage(
                        id = generateMessageId(),
                        timestamp = getCurrentTimeMillis(),
                        type = "Message",
                        content = message.text ?: "[${message.messageType}]",
                        rawMessage = message.toString()
                    )
                )
            } else {
                addSocketMessageInternal(
                    SocketMessage(
                        id = generateMessageId(),
                        timestamp = getCurrentTimeMillis(),
                        type = "MessageEvent",
                        content = messageEvent::class.simpleName ?: "Unknown Event",
                        rawMessage = messageEvent.toString()
                    )
                )
            }
        }
        
        client?.eventListener = { event ->
            addSocketMessageInternal(
                SocketMessage(
                    id = generateMessageId(),
                    timestamp = getCurrentTimeMillis(),
                    type = "Event",
                    content = event.toString(),
                    rawMessage = event.toString()
                )
            )
        }
    }
    
    /**
     * Load saved attachments from platform storage
     */
    private fun loadSavedAttachments(platformContext: PlatformContext) {
        val savedAttachments = platformContext.loadSavedAttachments()
        _savedAttachments.value = savedAttachments
    }
    
    /**
     * Fetch deployment configuration
     */
    private fun fetchDeploymentConfig() {
        scope.launch {
            try {
                messengerTransport?.fetchDeploymentConfig()
                addSocketMessageInternal(
                    SocketMessage(
                        id = generateMessageId(),
                        timestamp = getCurrentTimeMillis(),
                        type = "System",
                        content = "Deployment config fetched successfully",
                        rawMessage = "Deployment configuration loaded"
                    )
                )
            } catch (e: Exception) {
                addSocketMessageInternal(
                    SocketMessage(
                        id = generateMessageId(),
                        timestamp = getCurrentTimeMillis(),
                        type = "Error",
                        content = "Failed to fetch deployment config: ${e.message}",
                        rawMessage = "DeploymentConfig error: ${e.message}"
                    )
                )
            }
        }
    }
    
    /**
     * Execute a command
     */
    fun executeCommand(commandText: String) {
        if (commandWaiting) return
        
        commandWaiting = true
        
        scope.launch {
            try {
                when (commandText.lowercase()) {
                    "connect" -> client?.connect()
                    "disconnect" -> client?.disconnect()
                    "send hello" -> client?.sendMessage("Hello from TestBed!")
                    "health" -> client?.sendHealthCheck()
                    "typing" -> client?.indicateTyping()
                    else -> {
                        addSocketMessageInternal(
                            SocketMessage(
                                id = generateMessageId(),
                                timestamp = getCurrentTimeMillis(),
                                type = "Error",
                                content = "Unknown command: $commandText",
                                rawMessage = "Command not recognized"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                addSocketMessageInternal(
                    SocketMessage(
                        id = generateMessageId(),
                        timestamp = getCurrentTimeMillis(),
                        type = "Error",
                        content = "Command failed: ${e.message}",
                        rawMessage = "Exception: ${e.message}"
                    )
                )
            } finally {
                commandWaiting = false
            }
        }
    }
    
    /**
     * Add a socket message to the list (private implementation)
     */
    private fun addSocketMessageInternal(message: SocketMessage) {
        _socketMessages.value = _socketMessages.value + message
    }
    
    /**
     * Generate a unique message ID
     */
    private fun generateMessageId(): String = "msg_${getCurrentTimeMillis()}"
    
    /**
     * Handle errors
     */
    private fun handleError(error: TestBedError) {
        errorHandler.handleError(error)
        addSocketMessageInternal(
            SocketMessage(
                id = generateMessageId(),
                timestamp = getCurrentTimeMillis(),
                type = "Error",
                content = error.message,
                rawMessage = error.toString()
            )
        )
    }
    
    /**
     * Create available commands list
     */
    private fun createAvailableCommands(): List<Command> {
        return listOf(
            Command("connect", "Connect to messaging service"),
            Command("disconnect", "Disconnect from messaging service"),
            Command("send hello", "Send a test message"),
            Command("health", "Send health check"),
            Command("typing", "Send typing indicator")
        )
    }
    
    /**
     * Handle command change
     */
    fun onCommandChanged(newCommand: String) {
        command = newCommand
    }
    
    /**
     * Handle command send
     */
    fun onCommandSend() {
        if (command.isNotBlank() && !commandWaiting) {
            executeCommand(command)
        }
    }
    
    /**
     * Clear errors
     */
    fun clearErrors() {
        errorHandler.clearErrorHistory()
    }
    
    /**
     * Add a socket message (public method for external use)
     */
    fun addSocketMessage(message: SocketMessage) {
        _socketMessages.value = _socketMessages.value + message
    }
}