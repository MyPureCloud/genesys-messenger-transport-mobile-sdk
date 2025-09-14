package com.genesys.cloud.messenger.composeapp.model

/**
 * Sealed class representing different types of errors that can occur in the application.
 * This provides a type-safe way to handle various error scenarios.
 */
sealed class AppError(
    open val message: String,
    open val cause: Throwable? = null
) {
    
    /**
     * Network-related errors
     */
    sealed class NetworkError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause) {
        
        data class ConnectionError(
            override val message: String = "Unable to connect to the server. Please check your internet connection.",
            override val cause: Throwable? = null
        ) : NetworkError(message, cause)
        
        data class TimeoutError(
            override val message: String = "Request timed out. Please try again.",
            override val cause: Throwable? = null
        ) : NetworkError(message, cause)
        
        data class ServerError(
            val statusCode: Int,
            override val message: String = "Server error occurred. Please try again later.",
            override val cause: Throwable? = null
        ) : NetworkError(message, cause)
        
        data class UnauthorizedError(
            override val message: String = "Authentication failed. Please log in again.",
            override val cause: Throwable? = null
        ) : NetworkError(message, cause)
    }
    
    /**
     * Input validation errors
     */
    sealed class ValidationError(
        override val message: String,
        val field: String? = null
    ) : AppError(message) {
        
        data class EmptyFieldError(
            val fieldName: String,
            override val message: String = "$fieldName cannot be empty"
        ) : ValidationError(message, fieldName)
        
        data class InvalidFormatError(
            val fieldName: String,
            override val message: String = "$fieldName has invalid format"
        ) : ValidationError(message, fieldName)
        
        data class TooLongError(
            val fieldName: String,
            val maxLength: Int,
            override val message: String = "$fieldName cannot exceed $maxLength characters"
        ) : ValidationError(message, fieldName)
        
        data class TooShortError(
            val fieldName: String,
            val minLength: Int,
            override val message: String = "$fieldName must be at least $minLength characters"
        ) : ValidationError(message, fieldName)
        
        data class InvalidCharactersError(
            val fieldName: String,
            override val message: String = "$fieldName contains invalid characters"
        ) : ValidationError(message, fieldName)
    }
    
    /**
     * Platform-specific errors
     */
    sealed class PlatformError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause) {
        
        data class StorageError(
            override val message: String = "Failed to access device storage",
            override val cause: Throwable? = null
        ) : PlatformError(message, cause)
        
        data class PermissionError(
            val permission: String,
            override val message: String = "Permission required: $permission",
            override val cause: Throwable? = null
        ) : PlatformError(message, cause)
        
        data class NotificationError(
            override val message: String = "Failed to handle notifications",
            override val cause: Throwable? = null
        ) : PlatformError(message, cause)
    }
    
    /**
     * Business logic errors
     */
    sealed class BusinessError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause) {
        
        data class MessageSendError(
            override val message: String = "Failed to send message. Please try again.",
            override val cause: Throwable? = null
        ) : BusinessError(message, cause)
        
        data class ConversationLoadError(
            override val message: String = "Failed to load conversation history",
            override val cause: Throwable? = null
        ) : BusinessError(message, cause)
        
        data class SettingsSaveError(
            override val message: String = "Failed to save settings",
            override val cause: Throwable? = null
        ) : BusinessError(message, cause)
        
        data class AuthenticationError(
            override val message: String = "Authentication failed",
            override val cause: Throwable? = null
        ) : BusinessError(message, cause)
    }
    
    /**
     * Generic unknown error
     */
    data class UnknownError(
        override val message: String = "An unexpected error occurred",
        override val cause: Throwable? = null
    ) : AppError(message, cause)
}

/**
 * Extension function to convert exceptions to AppError
 */
fun Throwable.toAppError(): AppError {
    return when {
        this::class.simpleName == "UnknownHostException" -> AppError.NetworkError.ConnectionError(cause = this)
        this::class.simpleName == "SocketTimeoutException" -> AppError.NetworkError.TimeoutError(cause = this)
        this::class.simpleName == "IOException" -> AppError.NetworkError.ConnectionError(
            message = "Network error: ${this.message ?: "Unknown network issue"}",
            cause = this
        )
        this::class.simpleName == "SecurityException" -> AppError.PlatformError.PermissionError(
            permission = "Unknown",
            message = "Security error: ${this.message ?: "Permission denied"}",
            cause = this
        )
        this.message?.contains("network", ignoreCase = true) == true -> AppError.NetworkError.ConnectionError(
            message = this.message ?: "Network error occurred",
            cause = this
        )
        this.message?.contains("timeout", ignoreCase = true) == true -> AppError.NetworkError.TimeoutError(
            message = this.message ?: "Operation timed out",
            cause = this
        )
        else -> AppError.UnknownError(
            message = this.message ?: "An unexpected error occurred",
            cause = this
        )
    }
}

/**
 * Result wrapper for operations that can fail
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val error: AppError) : Result<Nothing>()
    
    inline fun <R> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
        }
    }
    
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onError(action: (AppError) -> Unit): Result<T> {
        if (this is Error) action(error)
        return this
    }
}