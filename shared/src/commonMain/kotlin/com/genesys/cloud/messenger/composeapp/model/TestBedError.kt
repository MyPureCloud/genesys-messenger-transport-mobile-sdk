package com.genesys.cloud.messenger.composeapp.model

/**
 * TestBed-specific error hierarchy for handling errors in the TestBedViewModel.
 * Extends the base AppError class to provide specific error types for testbed operations.
 */
sealed class TestBedError(
    override val message: String,
    override val cause: Throwable? = null
) : AppError(message, cause) {
    
    /**
     * Command execution errors
     */
    sealed class CommandExecutionError(
        override val message: String,
        open val command: String,
        override val cause: Throwable? = null
    ) : TestBedError(message, cause) {
        
        data class InvalidCommandError(
            override val command: String,
            override val message: String = "Unknown command: $command"
        ) : CommandExecutionError(message, command)
        
        data class MissingParametersError(
            override val command: String,
            val requiredParameter: String,
            override val message: String = "Command '$command' requires parameter: $requiredParameter"
        ) : CommandExecutionError(message, command)
        
        data class InvalidParametersError(
            override val command: String,
            val parameter: String,
            val reason: String,
            override val message: String = "Invalid parameter '$parameter' for command '$command': $reason"
        ) : CommandExecutionError(message, command)
        
        data class CommandFailedError(
            override val command: String,
            val reason: String,
            override val message: String = "Command '$command' failed: $reason",
            override val cause: Throwable? = null
        ) : CommandExecutionError(message, command, cause)
        
        data class ClientNotInitializedError(
            override val command: String,
            override val message: String = "Cannot execute command '$command': client not initialized"
        ) : CommandExecutionError(message, command)
        
        data class ClientInvalidStateError(
            override val command: String,
            val currentState: String,
            val requiredState: String,
            override val message: String = "Cannot execute command '$command': client is in state '$currentState', requires '$requiredState'"
        ) : CommandExecutionError(message, command)
    }
    
    /**
     * Connection-related errors
     */
    sealed class ConnectionError(
        override val message: String,
        override val cause: Throwable? = null
    ) : TestBedError(message, cause) {
        
        data class InitializationError(
            val reason: String,
            override val message: String = "Failed to initialize messaging client: $reason",
            override val cause: Throwable? = null
        ) : ConnectionError(message, cause)
        
        data class ConnectError(
            val deploymentId: String,
            val region: String,
            override val message: String = "Failed to connect to deployment '$deploymentId' in region '$region'",
            override val cause: Throwable? = null
        ) : ConnectionError(message, cause)
        
        data class DisconnectError(
            override val message: String = "Failed to disconnect from messaging service",
            override val cause: Throwable? = null
        ) : ConnectionError(message, cause)
        
        data class ReconnectError(
            val attemptCount: Int,
            override val message: String = "Failed to reconnect after $attemptCount attempts",
            override val cause: Throwable? = null
        ) : ConnectionError(message, cause)
        
        data class NetworkUnavailableError(
            override val message: String = "Network connection is not available",
            override val cause: Throwable? = null
        ) : ConnectionError(message, cause)
        
        data class ServiceUnavailableError(
            val deploymentId: String,
            val region: String,
            override val message: String = "Messaging service unavailable for deployment '$deploymentId' in region '$region'",
            override val cause: Throwable? = null
        ) : ConnectionError(message, cause)
    }
    
    /**
     * Authentication-related errors
     */
    sealed class AuthenticationError(
        override val message: String,
        open val authState: AuthState? = null,
        override val cause: Throwable? = null
    ) : TestBedError(message, cause) {
        
        data class OktaSignInError(
            val reason: String,
            override val message: String = "OKTA sign-in failed: $reason",
            override val cause: Throwable? = null
        ) : AuthenticationError(message, null, cause)
        
        data class AuthCodeError(
            val authCode: String,
            override val message: String = "Invalid or expired authorization code",
            override val cause: Throwable? = null
        ) : AuthenticationError(message, null, cause)
        
        data class TokenError(
            val tokenType: String,
            override val message: String = "Failed to handle $tokenType token",
            override val cause: Throwable? = null
        ) : AuthenticationError(message, null, cause)
        
        data class StepUpError(
            override val message: String = "Step-up authentication failed",
            override val cause: Throwable? = null
        ) : AuthenticationError(message, null, cause)
        
        data class LogoutError(
            override val message: String = "Failed to logout from OKTA session",
            override val cause: Throwable? = null
        ) : AuthenticationError(message, null, cause)
        
        data class AuthStateError(
            override val authState: AuthState,
            val expectedState: AuthState,
            override val message: String = "Invalid authentication state: expected ${expectedState::class.simpleName}, got ${authState::class.simpleName}",
            override val cause: Throwable? = null
        ) : AuthenticationError(message, authState, cause)
    }
    
    /**
     * Deployment settings validation errors
     */
    sealed class DeploymentValidationError(
        override val message: String,
        val field: String
    ) : TestBedError(message) {
        
        data class EmptyDeploymentIdError(
            override val message: String = "Deployment ID cannot be empty"
        ) : DeploymentValidationError(message, "deploymentId")
        
        data class InvalidDeploymentIdError(
            val deploymentId: String,
            override val message: String = "Invalid deployment ID format: '$deploymentId'"
        ) : DeploymentValidationError(message, "deploymentId")
        
        data class EmptyRegionError(
            override val message: String = "Region cannot be empty"
        ) : DeploymentValidationError(message, "region")
        
        data class InvalidRegionError(
            val region: String,
            val availableRegions: List<String>,
            override val message: String = "Invalid region '$region'. Available regions: ${availableRegions.joinToString(", ")}"
        ) : DeploymentValidationError(message, "region")
        
        data class DeploymentConfigError(
            val deploymentId: String,
            val region: String,
            override val message: String = "Failed to validate deployment configuration for '$deploymentId' in '$region'",
            override val cause: Throwable? = null
        ) : DeploymentValidationError(message, "deployment")
    }
    
    /**
     * Attachment-related errors
     */
    sealed class AttachmentError(
        override val message: String,
        override val cause: Throwable? = null
    ) : TestBedError(message, cause) {
        
        data class FileNotFoundError(
            val fileName: String,
            override val message: String = "File not found: $fileName"
        ) : AttachmentError(message)
        
        data class FileTooLargeError(
            val fileName: String,
            val fileSize: Long,
            val maxSize: Long,
            override val message: String = "File '$fileName' is too large (${fileSize}B). Maximum size: ${maxSize}B"
        ) : AttachmentError(message)
        
        data class UnsupportedFileTypeError(
            val fileName: String,
            val fileType: String,
            override val message: String = "Unsupported file type '$fileType' for file '$fileName'"
        ) : AttachmentError(message)
        
        data class AttachmentUploadError(
            val fileName: String,
            override val message: String = "Failed to upload attachment: $fileName",
            override val cause: Throwable? = null
        ) : AttachmentError(message, cause)
        
        data class AttachmentDownloadError(
            val attachmentId: String,
            override val message: String = "Failed to download attachment: $attachmentId",
            override val cause: Throwable? = null
        ) : AttachmentError(message, cause)
    }
    
    /**
     * Push notification errors
     */
    sealed class PushNotificationError(
        override val message: String,
        override val cause: Throwable? = null
    ) : TestBedError(message, cause) {
        
        data class TokenRegistrationError(
            val token: String,
            override val message: String = "Failed to register push notification token",
            override val cause: Throwable? = null
        ) : PushNotificationError(message, cause)
        
        data class TokenUnregistrationError(
            override val message: String = "Failed to unregister from push notifications",
            override val cause: Throwable? = null
        ) : PushNotificationError(message, cause)
        
        data class PushServiceError(
            override val message: String = "Push notification service error",
            override val cause: Throwable? = null
        ) : PushNotificationError(message, cause)
    }
    
    /**
     * Socket message processing errors
     */
    sealed class MessageProcessingError(
        override val message: String,
        override val cause: Throwable? = null
    ) : TestBedError(message, cause) {
        
        data class MessageParsingError(
            val rawMessage: String,
            override val message: String = "Failed to parse message",
            override val cause: Throwable? = null
        ) : MessageProcessingError(message, cause)
        
        data class MessageFormattingError(
            val messageType: String,
            override val message: String = "Failed to format message of type '$messageType'",
            override val cause: Throwable? = null
        ) : MessageProcessingError(message, cause)
        
        data class EventProcessingError(
            val eventType: String,
            override val message: String = "Failed to process event of type '$eventType'",
            override val cause: Throwable? = null
        ) : MessageProcessingError(message, cause)
    }
}

/**
 * Extension function to convert TestBed-specific exceptions to TestBedError
 */
//fun Throwable.toTestBedError(context: String = ""): TestBedError {
//    return when {
//        // Check class name first, then message content
//        this::class.simpleName?.contains("Network", ignoreCase = true) == true ||
//        this.message?.contains("network", ignoreCase = true) == true ->
//            TestBedError.ConnectionError.NetworkUnavailableError(
//                message = "Network error${if (context.isNotEmpty()) " in $context" else ""}: ${this.message}",
//                cause = this
//            )
//
//        this::class.simpleName?.contains("Timeout", ignoreCase = true) == true ||
//        this.message?.contains("timeout", ignoreCase = true) == true ->
//            TestBedError.ConnectionError.ConnectError(
//                deploymentId = "unknown",
//                region = "unknown",
//                message = "Connection timeout${if (context.isNotEmpty()) " in $context" else ""}: ${this.message}",
//                cause = this
//            )
//
//        this::class.simpleName?.contains("Auth", ignoreCase = true) == true ||
//        this.message?.contains("auth", ignoreCase = true) == true ->
//            TestBedError.AuthenticationError.TokenError(
//                tokenType = "unknown",
//                message = "Authentication error${if (context.isNotEmpty()) " in $context" else ""}: ${this.message}",
//                cause = this
//            )
//
//        this::class.simpleName?.contains("File", ignoreCase = true) == true ||
//        this.message?.contains("file", ignoreCase = true) == true ->
//            TestBedError.AttachmentError.AttachmentUploadError(
//                fileName = "unknown",
//                message = "File error${if (context.isNotEmpty()) " in $context" else ""}: ${this.message}",
//                cause = this
//            )
//
//        this.message?.contains("command", ignoreCase = true) == true ->
//            TestBedError.CommandExecutionError.CommandFailedError(
//                command = "unknown",
//                reason = this.message ?: "Unknown error",
//                cause = this
//            )
//
//        else -> TestBedError.ConnectionError.InitializationError(
//            reason = "${if (context.isNotEmpty()) "$context: " else ""}${this.message ?: "Unknown error"}",
//            cause = this
//        )
//    }
//}

/**
 * Retry configuration for error handling
 */
data class RetryConfig(
    val maxAttempts: Int = 3,
    val delayMillis: Long = 1000,
    val backoffMultiplier: Double = 2.0,
    val maxDelayMillis: Long = 10000
)

/**
 * Error recovery actions
 */
sealed class ErrorRecoveryAction {
    object Retry : ErrorRecoveryAction()
    object Reconnect : ErrorRecoveryAction()
    object Reinitialize : ErrorRecoveryAction()
    object ShowError : ErrorRecoveryAction()
    object Ignore : ErrorRecoveryAction()
    data class Custom(val action: suspend () -> Unit) : ErrorRecoveryAction()
}