package com.genesys.cloud.messenger.composeapp.model

/**
 * Extension functions for converting exceptions to TestBedError
 */

/**
 * Convert a Throwable to TestBedError
 */
fun Throwable.toTestBedError(context: String = ""): TestBedError {
    return when (this) {
        is IllegalArgumentException -> TestBedError.CommandExecutionError.InvalidParametersError(
            command = context,
            parameter = "unknown",
            reason = message ?: "Invalid parameter"
        )
        
        is IllegalStateException -> TestBedError.CommandExecutionError.ClientInvalidStateError(
            command = context,
            currentState = "unknown",
            requiredState = "unknown"
        )
        
        // Network-related exceptions (check by message content since we can't use platform-specific types)
        else -> when {
            this.message?.contains("network", ignoreCase = true) == true ||
            this.message?.contains("connection", ignoreCase = true) == true ||
            this.message?.contains("timeout", ignoreCase = true) == true -> 
                TestBedError.ConnectionError.NetworkUnavailableError(cause = this)
            
            this.message?.contains("file", ignoreCase = true) == true ||
            this.message?.contains("not found", ignoreCase = true) == true -> 
                TestBedError.AttachmentError.FileNotFoundError(fileName = context)
            
            this.message?.contains("auth", ignoreCase = true) == true ||
            this.message?.contains("security", ignoreCase = true) == true -> 
                TestBedError.AuthenticationError.AuthStateError(
                    authState = AuthState.NoAuth,
                    expectedState = AuthState.Authorized,
                    cause = this
                )
            
            else -> TestBedError.CommandExecutionError.CommandFailedError(
                command = context,
                reason = message ?: "Unknown error",
                cause = this
            )
        }
        

    }
}

/**
 * Convert an Exception to TestBedError with command context
 */
fun Exception.toTestBedError(command: String): TestBedError {
    return (this as Throwable).toTestBedError(command)
}