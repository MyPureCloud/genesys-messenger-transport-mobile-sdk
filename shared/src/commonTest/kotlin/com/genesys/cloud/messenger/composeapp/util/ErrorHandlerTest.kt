package com.genesys.cloud.messenger.composeapp.util

import com.genesys.cloud.messenger.composeapp.model.TestBedError
import com.genesys.cloud.messenger.composeapp.model.ErrorRecoveryAction
import com.genesys.cloud.messenger.composeapp.model.RetryConfig
import com.genesys.cloud.messenger.composeapp.model.AuthState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class ErrorHandlerTest {
    
    private val errorHandler = ErrorHandler()
    
    @Test
    fun `handleError should return correct recovery action for command errors`() {
        val invalidCommandError = TestBedError.CommandExecutionError.InvalidCommandError("test")
        val recoveryAction = errorHandler.handleError(invalidCommandError)
        
        assertEquals(ErrorRecoveryAction.ShowError, recoveryAction)
    }
    
    @Test
    fun `handleError should return reconnect for connection errors`() {
        val connectionError = TestBedError.ConnectionError.ConnectError(
            deploymentId = "test-deployment",
            region = "us-east-1"
        )
        val recoveryAction = errorHandler.handleError(connectionError)
        
        assertEquals(ErrorRecoveryAction.Reconnect, recoveryAction)
    }
    
    @Test
    fun `handleError should return reinitialize for initialization errors`() {
        val initError = TestBedError.ConnectionError.InitializationError("Test initialization error")
        val recoveryAction = errorHandler.handleError(initError)
        
        assertEquals(ErrorRecoveryAction.Reinitialize, recoveryAction)
    }
    
    @Test
    fun `handleError should return retry for command failed errors`() {
        val commandFailedError = TestBedError.CommandExecutionError.CommandFailedError(
            command = "connect",
            reason = "Network timeout"
        )
        val recoveryAction = errorHandler.handleError(commandFailedError)
        
        assertEquals(ErrorRecoveryAction.Retry, recoveryAction)
    }
    
    @Test
    fun `handleError should return show error for authentication errors`() {
        val authError = TestBedError.AuthenticationError.OktaSignInError("Invalid credentials")
        val recoveryAction = errorHandler.handleError(authError)
        
        assertEquals(ErrorRecoveryAction.ShowError, recoveryAction)
    }
    
    @Test
    fun `handleError should return show error for validation errors`() {
        val validationError = TestBedError.DeploymentValidationError.EmptyDeploymentIdError()
        val recoveryAction = errorHandler.handleError(validationError)
        
        assertEquals(ErrorRecoveryAction.ShowError, recoveryAction)
    }
    
    @Test
    fun `handleError should return retry for attachment upload errors`() {
        val attachmentError = TestBedError.AttachmentError.AttachmentUploadError("test.jpg")
        val recoveryAction = errorHandler.handleError(attachmentError)
        
        assertEquals(ErrorRecoveryAction.Retry, recoveryAction)
    }
    
    @Test
    fun `handleError should return show error for file validation errors`() {
        val fileError = TestBedError.AttachmentError.FileTooLargeError(
            fileName = "large.jpg",
            fileSize = 10000000,
            maxSize = 5000000
        )
        val recoveryAction = errorHandler.handleError(fileError)
        
        assertEquals(ErrorRecoveryAction.ShowError, recoveryAction)
    }
    
    @Test
    fun `handleError should return ignore for message processing errors`() {
        val messageError = TestBedError.MessageProcessingError.MessageParsingError("invalid json")
        val recoveryAction = errorHandler.handleError(messageError)
        
        assertEquals(ErrorRecoveryAction.Ignore, recoveryAction)
    }
    
    @Test
    fun `handleError should track error history`() {
        val error1 = TestBedError.CommandExecutionError.InvalidCommandError("test1")
        val error2 = TestBedError.CommandExecutionError.InvalidCommandError("test2")
        
        errorHandler.handleError(error1)
        errorHandler.handleError(error2)
        
        assertEquals(2, errorHandler.errorHistory.value.size)
        assertEquals(error2, errorHandler.lastError.value)
    }
    
    @Test
    fun `executeWithRetry should succeed on first attempt`() = runTest {
        var attempts = 0
        val result = errorHandler.executeWithRetry(
            operation = {
                attempts++
                "success"
            },
            retryConfig = RetryConfig(maxAttempts = 3, delayMillis = 10)
        )
        
        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
        assertEquals(1, attempts)
    }
    
    @Test
    fun `executeWithRetry should retry on failure and eventually succeed`() = runTest {
        var attempts = 0
        val result = errorHandler.executeWithRetry(
            operation = {
                attempts++
                if (attempts < 3) {
                    throw RuntimeException("Temporary failure")
                }
                "success"
            },
            retryConfig = RetryConfig(maxAttempts = 3, delayMillis = 10)
        )
        
        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
        assertEquals(3, attempts)
    }
    
    @Test
    fun `executeWithRetry should fail after max attempts`() = runTest {
        var attempts = 0
        val result = errorHandler.executeWithRetry(
            operation = {
                attempts++
                throw RuntimeException("Persistent failure")
            },
            retryConfig = RetryConfig(maxAttempts = 2, delayMillis = 10)
        )
        
        assertTrue(result.isFailure)
        assertEquals(2, attempts)
    }
    
    @Test
    fun `createErrorSocketMessage should format error correctly`() {
        val error = TestBedError.CommandExecutionError.InvalidCommandError("invalidcmd")
        val socketMessage = errorHandler.createErrorSocketMessage(error)
        
        assertEquals("Command Error", socketMessage.type)
        assertEquals(error.message, socketMessage.content)
        assertTrue(socketMessage.rawMessage.contains("Command: invalidcmd"))
    }
    
    @Test
    fun `createErrorSocketMessage should format connection error correctly`() {
        val error = TestBedError.ConnectionError.ConnectError(
            deploymentId = "test-deployment",
            region = "us-east-1",
            message = "Connection failed"
        )
        val socketMessage = errorHandler.createErrorSocketMessage(error)
        
        assertEquals("Connection Error", socketMessage.type)
        assertEquals("Connection failed", socketMessage.content)
        assertTrue(socketMessage.rawMessage.contains("Deployment ID: test-deployment"))
        assertTrue(socketMessage.rawMessage.contains("Region: us-east-1"))
    }
    
    @Test
    fun `getErrorMessageWithSuggestions should provide helpful suggestions`() {
        val error = TestBedError.CommandExecutionError.InvalidCommandError("badcmd")
        val messageWithSuggestions = errorHandler.getErrorMessageWithSuggestions(error)
        
        assertTrue(messageWithSuggestions.contains(error.message))
        assertTrue(messageWithSuggestions.contains("Suggestion:"))
        assertTrue(messageWithSuggestions.contains("dropdown"))
    }
    
    @Test
    fun `isRecoverable should return false for validation errors`() {
        val validationError = TestBedError.DeploymentValidationError.EmptyDeploymentIdError()
        val isRecoverable = errorHandler.isRecoverable(validationError)
        
        assertEquals(false, isRecoverable)
    }
    
    @Test
    fun `isRecoverable should return true for connection errors`() {
        val connectionError = TestBedError.ConnectionError.ConnectError("test", "us-east-1")
        val isRecoverable = errorHandler.isRecoverable(connectionError)
        
        assertEquals(true, isRecoverable)
    }
    
    @Test
    fun `getRetryConfig should return appropriate config for different error types`() {
        val connectionError = TestBedError.ConnectionError.ConnectError("test", "us-east-1")
        val connectionConfig = errorHandler.getRetryConfig(connectionError)
        
        assertEquals(3, connectionConfig.maxAttempts)
        assertEquals(2000, connectionConfig.delayMillis)
        
        val commandError = TestBedError.CommandExecutionError.CommandFailedError("test", "failed")
        val commandConfig = errorHandler.getRetryConfig(commandError)
        
        assertEquals(2, commandConfig.maxAttempts)
        assertEquals(1000, commandConfig.delayMillis)
    }
    
    @Test
    fun `clearErrorHistory should clear all errors`() {
        val error = TestBedError.CommandExecutionError.InvalidCommandError("test")
        errorHandler.handleError(error)
        
        assertEquals(1, errorHandler.errorHistory.value.size)
        assertNotNull(errorHandler.lastError.value)
        
        errorHandler.clearErrorHistory()
        
        assertEquals(0, errorHandler.errorHistory.value.size)
        assertEquals(null, errorHandler.lastError.value)
    }
}