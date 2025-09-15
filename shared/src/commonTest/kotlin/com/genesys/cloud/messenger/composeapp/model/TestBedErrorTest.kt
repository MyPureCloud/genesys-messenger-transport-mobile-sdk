package com.genesys.cloud.messenger.composeapp.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class TestBedErrorTest {
    
    @Test
    fun `toTestBedError should convert network exceptions correctly`() {
        val networkException = RuntimeException("Network error occurred")
        val testBedError = networkException.toTestBedError("test context")
        
        assertTrue(testBedError is TestBedError.ConnectionError.NetworkUnavailableError)
        assertTrue(testBedError.message.contains("test context"))
        assertTrue(testBedError.message.contains("Network error occurred"))
        assertEquals(networkException, testBedError.cause)
    }
    
    @Test
    fun `toTestBedError should convert timeout exceptions correctly`() {
        val timeoutException = RuntimeException("Connection timeout")
        val testBedError = timeoutException.toTestBedError("connection")
        
        assertTrue(testBedError is TestBedError.ConnectionError.ConnectError)
        assertTrue(testBedError.message.contains("connection"))
        assertTrue(testBedError.message.contains("Connection timeout"))
        assertEquals(timeoutException, testBedError.cause)
    }
    
    @Test
    fun `toTestBedError should convert auth exceptions correctly`() {
        val authException = RuntimeException("Authentication failed")
        val testBedError = authException.toTestBedError("auth flow")
        
        assertTrue(testBedError is TestBedError.AuthenticationError.TokenError)
        assertTrue(testBedError.message.contains("auth flow"))
        assertTrue(testBedError.message.contains("Authentication failed"))
        assertEquals(authException, testBedError.cause)
    }
    
    @Test
    fun `toTestBedError should convert file exceptions correctly`() {
        val fileException = RuntimeException("File not found")
        val testBedError = fileException.toTestBedError("file upload")
        
        assertTrue(testBedError is TestBedError.AttachmentError.AttachmentUploadError)
        assertTrue(testBedError.message.contains("file upload"))
        assertTrue(testBedError.message.contains("File not found"))
        assertEquals(fileException, testBedError.cause)
    }
    
    @Test
    fun `toTestBedError should convert command exceptions correctly`() {
        val commandException = RuntimeException("Command execution failed")
        val testBedError = commandException.toTestBedError("command processing")
        
        assertTrue(testBedError is TestBedError.CommandExecutionError.CommandFailedError)
        assertEquals("Command execution failed", testBedError.reason)
        assertEquals(commandException, testBedError.cause)
    }
    
    @Test
    fun `toTestBedError should handle unknown exceptions`() {
        val unknownException = RuntimeException("Unknown error")
        val testBedError = unknownException.toTestBedError("unknown context")
        
        assertTrue(testBedError is TestBedError.ConnectionError.InitializationError)
        assertTrue(testBedError.reason.contains("unknown context"))
        assertTrue(testBedError.reason.contains("Unknown error"))
        assertEquals(unknownException, testBedError.cause)
    }
    
    @Test
    fun `toTestBedError should work without context`() {
        val exception = RuntimeException("Test error")
        val testBedError = exception.toTestBedError()
        
        assertNotNull(testBedError)
        assertTrue(testBedError.message.contains("Test error"))
        assertEquals(exception, testBedError.cause)
    }
    
    @Test
    fun `CommandExecutionError should contain command information`() {
        val error = TestBedError.CommandExecutionError.InvalidCommandError("testcmd")
        
        assertEquals("testcmd", error.command)
        assertTrue(error.message.contains("testcmd"))
    }
    
    @Test
    fun `ConnectionError should contain deployment information`() {
        val error = TestBedError.ConnectionError.ConnectError(
            deploymentId = "test-deployment",
            region = "us-east-1"
        )
        
        assertEquals("test-deployment", error.deploymentId)
        assertEquals("us-east-1", error.region)
        assertTrue(error.message.contains("test-deployment"))
        assertTrue(error.message.contains("us-east-1"))
    }
    
    @Test
    fun `AuthenticationError should contain auth state information`() {
        val authState = AuthState.NoAuth
        val error = TestBedError.AuthenticationError.AuthStateError(
            authState = authState,
            expectedState = "Authorized"
        )
        
        assertEquals(authState, error.authState)
        assertEquals("Authorized", error.expectedState)
        assertTrue(error.message.contains("Authorized"))
    }
    
    @Test
    fun `DeploymentValidationError should contain field information`() {
        val error = TestBedError.DeploymentValidationError.InvalidRegionError(
            region = "invalid-region",
            availableRegions = listOf("us-east-1", "eu-west-1")
        )
        
        assertEquals("region", error.field)
        assertEquals("invalid-region", error.region)
        assertEquals(listOf("us-east-1", "eu-west-1"), error.availableRegions)
        assertTrue(error.message.contains("invalid-region"))
    }
    
    @Test
    fun `AttachmentError should contain file information`() {
        val error = TestBedError.AttachmentError.FileTooLargeError(
            fileName = "large-file.jpg",
            fileSize = 10000000,
            maxSize = 5000000
        )
        
        assertEquals("large-file.jpg", error.fileName)
        assertEquals(10000000, error.fileSize)
        assertEquals(5000000, error.maxSize)
        assertTrue(error.message.contains("large-file.jpg"))
    }
    
    @Test
    fun `RetryConfig should have reasonable defaults`() {
        val config = RetryConfig()
        
        assertEquals(3, config.maxAttempts)
        assertEquals(1000, config.delayMillis)
        assertEquals(2.0, config.backoffMultiplier)
        assertEquals(10000, config.maxDelayMillis)
    }
    
    @Test
    fun `RetryConfig should allow customization`() {
        val config = RetryConfig(
            maxAttempts = 5,
            delayMillis = 2000,
            backoffMultiplier = 1.5,
            maxDelayMillis = 15000
        )
        
        assertEquals(5, config.maxAttempts)
        assertEquals(2000, config.delayMillis)
        assertEquals(1.5, config.backoffMultiplier)
        assertEquals(15000, config.maxDelayMillis)
    }
    
    @Test
    fun `ErrorRecoveryAction should have all expected types`() {
        val actions = listOf(
            ErrorRecoveryAction.Retry,
            ErrorRecoveryAction.Reconnect,
            ErrorRecoveryAction.Reinitialize,
            ErrorRecoveryAction.ShowError,
            ErrorRecoveryAction.Ignore,
            ErrorRecoveryAction.Custom { /* no-op */ }
        )
        
        assertEquals(6, actions.size)
        assertTrue(actions[0] is ErrorRecoveryAction.Retry)
        assertTrue(actions[1] is ErrorRecoveryAction.Reconnect)
        assertTrue(actions[2] is ErrorRecoveryAction.Reinitialize)
        assertTrue(actions[3] is ErrorRecoveryAction.ShowError)
        assertTrue(actions[4] is ErrorRecoveryAction.Ignore)
        assertTrue(actions[5] is ErrorRecoveryAction.Custom)
    }
}