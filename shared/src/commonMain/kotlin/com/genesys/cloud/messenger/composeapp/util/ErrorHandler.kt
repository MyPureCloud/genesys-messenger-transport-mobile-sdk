package com.genesys.cloud.messenger.composeapp.util

import com.genesys.cloud.messenger.composeapp.model.TestBedError
import com.genesys.cloud.messenger.composeapp.model.RetryConfig
import com.genesys.cloud.messenger.composeapp.model.ErrorRecoveryAction
import com.genesys.cloud.messenger.composeapp.model.SocketMessage
import com.genesys.cloud.messenger.composeapp.model.toTestBedError
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized error handler for TestBed operations.
 * Provides error categorization, recovery strategies, and retry mechanisms.
 */
class ErrorHandler {
    
    private val _lastError = MutableStateFlow<TestBedError?>(null)
    val lastError: StateFlow<TestBedError?> = _lastError.asStateFlow()
    
    private val _errorHistory = MutableStateFlow<List<TestBedError>>(emptyList())
    val errorHistory: StateFlow<List<TestBedError>> = _errorHistory.asStateFlow()
    
    /**
     * Handle an error and determine the appropriate recovery action
     */
    fun handleError(error: TestBedError): ErrorRecoveryAction {
        // Record the error
        _lastError.value = error
        _errorHistory.value = _errorHistory.value + error
        
        // Determine recovery action based on error type
        return when (error) {
            // Connection errors - try to reconnect
            is TestBedError.ConnectionError.ConnectError,
            is TestBedError.ConnectionError.NetworkUnavailableError -> ErrorRecoveryAction.Reconnect
            
            // Initialization errors - reinitialize
            is TestBedError.ConnectionError.InitializationError -> ErrorRecoveryAction.Reinitialize
            
            // Command execution errors - show error, don't retry automatically
            is TestBedError.CommandExecutionError.InvalidCommandError,
            is TestBedError.CommandExecutionError.MissingParametersError,
            is TestBedError.CommandExecutionError.InvalidParametersError -> ErrorRecoveryAction.ShowError
            
            // Client state errors - show error and suggest reconnection
            is TestBedError.CommandExecutionError.ClientNotInitializedError,
            is TestBedError.CommandExecutionError.ClientInvalidStateError -> ErrorRecoveryAction.ShowError
            
            // Command failures - retry with backoff
            is TestBedError.CommandExecutionError.CommandFailedError -> ErrorRecoveryAction.Retry
            
            // Authentication errors - show error, require user action
            is TestBedError.AuthenticationError -> ErrorRecoveryAction.ShowError
            
            // Deployment validation errors - show error, require user correction
            is TestBedError.DeploymentValidationError -> ErrorRecoveryAction.ShowError
            
            // Attachment errors - retry for upload/download, show error for validation
            is TestBedError.AttachmentError.AttachmentUploadError,
            is TestBedError.AttachmentError.AttachmentDownloadError -> ErrorRecoveryAction.Retry
            
            is TestBedError.AttachmentError.FileNotFoundError,
            is TestBedError.AttachmentError.FileTooLargeError,
            is TestBedError.AttachmentError.UnsupportedFileTypeError -> ErrorRecoveryAction.ShowError
            
            // Push notification errors - retry
            is TestBedError.PushNotificationError -> ErrorRecoveryAction.Retry
            
            // Message processing errors - ignore and continue
            is TestBedError.MessageProcessingError -> ErrorRecoveryAction.Ignore
            
            // Reconnection errors - show error after multiple attempts
            is TestBedError.ConnectionError.ReconnectError -> ErrorRecoveryAction.ShowError
            
            // Service unavailable - show error
            is TestBedError.ConnectionError.ServiceUnavailableError -> ErrorRecoveryAction.ShowError
            
            // Disconnect errors - ignore (might be intentional)
            is TestBedError.ConnectionError.DisconnectError -> ErrorRecoveryAction.Ignore
            
            // Handle all other specific error types
            is TestBedError.AuthenticationError.OktaSignInError,
            is TestBedError.AuthenticationError.AuthCodeError,
            is TestBedError.AuthenticationError.TokenError,
            is TestBedError.AuthenticationError.StepUpError,
            is TestBedError.AuthenticationError.LogoutError,
            is TestBedError.AuthenticationError.AuthStateError -> ErrorRecoveryAction.ShowError
            
            is TestBedError.DeploymentValidationError.EmptyDeploymentIdError,
            is TestBedError.DeploymentValidationError.InvalidDeploymentIdError,
            is TestBedError.DeploymentValidationError.EmptyRegionError,
            is TestBedError.DeploymentValidationError.InvalidRegionError,
            is TestBedError.DeploymentValidationError.DeploymentConfigError -> ErrorRecoveryAction.ShowError
            
            is TestBedError.PushNotificationError.TokenRegistrationError,
            is TestBedError.PushNotificationError.TokenUnregistrationError,
            is TestBedError.PushNotificationError.PushServiceError -> ErrorRecoveryAction.Retry
            
            is TestBedError.MessageProcessingError.MessageParsingError,
            is TestBedError.MessageProcessingError.MessageFormattingError,
            is TestBedError.MessageProcessingError.EventProcessingError -> ErrorRecoveryAction.Ignore
        }
    }
    
    /**
     * Execute an operation with retry logic
     */
    suspend fun <T> executeWithRetry(
        operation: suspend () -> T,
        retryConfig: RetryConfig = RetryConfig(),
        onError: (TestBedError, Int) -> Unit = { _, _ -> }
    ): Result<T> {
        var lastError: TestBedError? = null
        var currentDelay = retryConfig.delayMillis
        
        repeat(retryConfig.maxAttempts) { attempt ->
            try {
                return Result.success(operation())
            } catch (e: Exception) {
                val testBedError = e.toTestBedError("retry attempt ${attempt + 1}")
                lastError = testBedError
                onError(testBedError, attempt + 1)
                
                // Don't delay on the last attempt
                if (attempt < retryConfig.maxAttempts - 1) {
                    delay(currentDelay)
                    currentDelay = (currentDelay * retryConfig.backoffMultiplier).toLong()
                        .coerceAtMost(retryConfig.maxDelayMillis)
                }
            }
        }
        
        return Result.failure(Exception(lastError?.message ?: "Unknown error"))
    }
    
    /**
     * Create a socket message from an error for display
     */
    fun createErrorSocketMessage(error: TestBedError): SocketMessage {
        val errorType = when (error) {
            is TestBedError.CommandExecutionError -> "Command Error"
            is TestBedError.ConnectionError -> "Connection Error"
            is TestBedError.AuthenticationError -> "Authentication Error"
            is TestBedError.DeploymentValidationError -> "Validation Error"
            is TestBedError.AttachmentError -> "Attachment Error"
            is TestBedError.PushNotificationError -> "Push Notification Error"
            is TestBedError.MessageProcessingError -> "Message Processing Error"
        }
        
        val errorDetails = buildString {
            appendLine("Error Type: ${error::class.simpleName}")
            appendLine("Message: ${error.message}")
            
            when (error) {
                is TestBedError.CommandExecutionError -> {
                    appendLine("Command: ${error.command}")
                    if (error is TestBedError.CommandExecutionError.InvalidParametersError) {
                        appendLine("Parameter: ${error.parameter}")
                        appendLine("Reason: ${error.reason}")
                    }
                }
                is TestBedError.ConnectionError.ConnectError -> {
                    appendLine("Deployment ID: ${error.deploymentId}")
                    appendLine("Region: ${error.region}")
                }
                is TestBedError.ConnectionError.ReconnectError -> {
                    appendLine("Attempt Count: ${error.attemptCount}")
                }
                is TestBedError.AuthenticationError.AuthStateError -> {
                    appendLine("Current State: ${error.authState}")
                    appendLine("Expected State: ${error.expectedState}")
                }
                is TestBedError.DeploymentValidationError -> {
                    appendLine("Field: ${error.field}")
                    if (error is TestBedError.DeploymentValidationError.InvalidRegionError) {
                        appendLine("Available Regions: ${error.availableRegions.joinToString(", ")}")
                    }
                }
                is TestBedError.AttachmentError.FileTooLargeError -> {
                    appendLine("File Name: ${error.fileName}")
                    appendLine("File Size: ${error.fileSize} bytes")
                    appendLine("Max Size: ${error.maxSize} bytes")
                }
                is TestBedError.AttachmentError.UnsupportedFileTypeError -> {
                    appendLine("File Name: ${error.fileName}")
                    appendLine("File Type: ${error.fileType}")
                }
                // Handle all other specific error types
                is TestBedError.AttachmentError.FileNotFoundError -> {
                    appendLine("File Name: ${error.fileName}")
                }
                is TestBedError.AttachmentError.AttachmentUploadError -> {
                    appendLine("File Name: ${error.fileName}")
                }
                is TestBedError.AttachmentError.AttachmentDownloadError -> {
                    appendLine("Attachment ID: ${error.attachmentId}")
                }
                is TestBedError.AuthenticationError.OktaSignInError -> {
                    appendLine("Reason: ${error.reason}")
                }
                is TestBedError.AuthenticationError.AuthCodeError -> {
                    appendLine("Auth Code: ${error.authCode}")
                }
                is TestBedError.AuthenticationError.TokenError -> {
                    appendLine("Token Type: ${error.tokenType}")
                }
                is TestBedError.AuthenticationError.StepUpError,
                is TestBedError.AuthenticationError.LogoutError -> {
                    // No additional details needed
                }
                is TestBedError.ConnectionError.InitializationError -> {
                    appendLine("Reason: ${error.reason}")
                }
                is TestBedError.ConnectionError.DisconnectError,
                is TestBedError.ConnectionError.NetworkUnavailableError,
                is TestBedError.ConnectionError.ServiceUnavailableError -> {
                    // No additional details needed
                }
                is TestBedError.DeploymentValidationError.EmptyDeploymentIdError,
                is TestBedError.DeploymentValidationError.EmptyRegionError -> {
                    // No additional details needed
                }
                is TestBedError.DeploymentValidationError.InvalidDeploymentIdError -> {
                    appendLine("Deployment ID: ${error.deploymentId}")
                }
                is TestBedError.DeploymentValidationError.DeploymentConfigError -> {
                    appendLine("Deployment ID: ${error.deploymentId}")
                    appendLine("Region: ${error.region}")
                }
                is TestBedError.PushNotificationError.TokenRegistrationError -> {
                    appendLine("Token: ${error.token}")
                }
                is TestBedError.PushNotificationError.TokenUnregistrationError,
                is TestBedError.PushNotificationError.PushServiceError -> {
                    // No additional details needed
                }
                is TestBedError.MessageProcessingError.MessageParsingError -> {
                    appendLine("Raw Message: ${error.rawMessage}")
                }
                is TestBedError.MessageProcessingError.MessageFormattingError -> {
                    appendLine("Message Type: ${error.messageType}")
                }
                is TestBedError.MessageProcessingError.EventProcessingError -> {
                    appendLine("Event Type: ${error.eventType}")
                }
            }
            
            error.cause?.let { cause ->
                appendLine("Underlying Cause: ${cause::class.simpleName}")
                appendLine("Cause Message: ${cause.message}")
            }
            
            appendLine("Timestamp: ${formatTimestamp(getCurrentTimeMillis())}")
        }
        
        return SocketMessage(
            id = "error_${getCurrentTimeMillis()}_${error.hashCode()}",
            timestamp = getCurrentTimeMillis(),
            type = errorType,
            content = error.message,
            rawMessage = errorDetails
        )
    }
    
    /**
     * Get user-friendly error message with suggested actions
     */
    fun getErrorMessageWithSuggestions(error: TestBedError): String {
        val suggestions = when (error) {
            is TestBedError.CommandExecutionError.InvalidCommandError -> 
                "Check available commands in the dropdown menu."
            
            is TestBedError.CommandExecutionError.MissingParametersError -> 
                "Provide the required parameter: ${error.requiredParameter}"
            
            is TestBedError.CommandExecutionError.ClientNotInitializedError -> 
                "Initialize the client first by checking your deployment settings."
            
            is TestBedError.ConnectionError.ConnectError -> 
                "Check your network connection and verify deployment settings."
            
            is TestBedError.ConnectionError.NetworkUnavailableError -> 
                "Check your internet connection and try again."
            
            is TestBedError.AuthenticationError.OktaSignInError -> 
                "Verify your OKTA credentials and try signing in again."
            
            is TestBedError.DeploymentValidationError.EmptyDeploymentIdError -> 
                "Enter a valid deployment ID in the settings."
            
            is TestBedError.DeploymentValidationError.InvalidRegionError -> 
                "Select a valid region: ${error.availableRegions.joinToString(", ")}"
            
            is TestBedError.AttachmentError.FileTooLargeError -> 
                "Choose a smaller file (max ${error.maxSize} bytes)."
            
            is TestBedError.AttachmentError.UnsupportedFileTypeError -> 
                "Use a supported file format."
            
            else -> "Please try again or contact support if the issue persists."
        }
        
        return "${error.message}\n\nSuggestion: $suggestions"
    }
    
    /**
     * Clear error history
     */
    fun clearErrorHistory() {
        _errorHistory.value = emptyList()
        _lastError.value = null
    }
    
    /**
     * Check if an error is recoverable
     */
    fun isRecoverable(error: TestBedError): Boolean {
        return when (error) {
            is TestBedError.CommandExecutionError.InvalidCommandError,
            is TestBedError.CommandExecutionError.MissingParametersError,
            is TestBedError.CommandExecutionError.InvalidParametersError,
            is TestBedError.DeploymentValidationError,
            is TestBedError.AttachmentError.FileNotFoundError,
            is TestBedError.AttachmentError.FileTooLargeError,
            is TestBedError.AttachmentError.UnsupportedFileTypeError -> false
            
            else -> true
        }
    }
    
    /**
     * Get retry configuration based on error type
     */
    fun getRetryConfig(error: TestBedError): RetryConfig {
        return when (error) {
            is TestBedError.ConnectionError -> RetryConfig(
                maxAttempts = 3,
                delayMillis = 2000,
                backoffMultiplier = 2.0,
                maxDelayMillis = 10000
            )
            
            is TestBedError.CommandExecutionError.CommandFailedError -> RetryConfig(
                maxAttempts = 2,
                delayMillis = 1000,
                backoffMultiplier = 1.5,
                maxDelayMillis = 5000
            )
            
            is TestBedError.AttachmentError -> RetryConfig(
                maxAttempts = 3,
                delayMillis = 1500,
                backoffMultiplier = 2.0,
                maxDelayMillis = 8000
            )
            
            is TestBedError.PushNotificationError -> RetryConfig(
                maxAttempts = 2,
                delayMillis = 1000,
                backoffMultiplier = 1.5,
                maxDelayMillis = 3000
            )
            
            else -> RetryConfig() // Default configuration
        }
    }
}