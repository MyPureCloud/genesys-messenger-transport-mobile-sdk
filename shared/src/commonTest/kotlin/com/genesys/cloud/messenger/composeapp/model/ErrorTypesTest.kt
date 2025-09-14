package com.genesys.cloud.messenger.composeapp.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ErrorTypesTest {
    
    @Test
    fun testNetworkErrorConnectionError() {
        val error = AppError.NetworkError.ConnectionError()
        
        assertTrue(error is AppError.NetworkError)
        assertTrue(error is AppError.NetworkError.ConnectionError)
        assertEquals("Unable to connect to the server. Please check your internet connection.", error.message)
        assertNull(error.cause)
    }
    
    @Test
    fun testNetworkErrorConnectionErrorWithCustomMessage() {
        val customMessage = "Custom connection error"
        val cause = Exception("Test cause")
        val error = AppError.NetworkError.ConnectionError(customMessage, cause)
        
        assertEquals(customMessage, error.message)
        assertEquals(cause, error.cause)
    }
    
    @Test
    fun testNetworkErrorTimeoutError() {
        val error = AppError.NetworkError.TimeoutError()
        
        assertTrue(error is AppError.NetworkError.TimeoutError)
        assertEquals("Request timed out. Please try again.", error.message)
    }
    
    @Test
    fun testNetworkErrorServerError() {
        val error = AppError.NetworkError.ServerError(500)
        
        assertTrue(error is AppError.NetworkError.ServerError)
        assertEquals(500, error.statusCode)
        assertEquals("Server error occurred. Please try again later.", error.message)
    }
    
    @Test
    fun testNetworkErrorUnauthorizedError() {
        val error = AppError.NetworkError.UnauthorizedError()
        
        assertTrue(error is AppError.NetworkError.UnauthorizedError)
        assertEquals("Authentication failed. Please log in again.", error.message)
    }
    
    @Test
    fun testValidationErrorEmptyFieldError() {
        val error = AppError.ValidationError.EmptyFieldError("Username")
        
        assertTrue(error is AppError.ValidationError.EmptyFieldError)
        assertEquals("Username", error.fieldName)
        assertEquals("Username cannot be empty", error.message)
        assertEquals("Username", error.field)
    }
    
    @Test
    fun testValidationErrorInvalidFormatError() {
        val error = AppError.ValidationError.InvalidFormatError("Email")
        
        assertTrue(error is AppError.ValidationError.InvalidFormatError)
        assertEquals("Email", error.fieldName)
        assertEquals("Email has invalid format", error.message)
    }
    
    @Test
    fun testValidationErrorTooLongError() {
        val error = AppError.ValidationError.TooLongError("Password", 50)
        
        assertTrue(error is AppError.ValidationError.TooLongError)
        assertEquals("Password", error.fieldName)
        assertEquals(50, error.maxLength)
        assertEquals("Password cannot exceed 50 characters", error.message)
    }
    
    @Test
    fun testValidationErrorTooShortError() {
        val error = AppError.ValidationError.TooShortError("Password", 8)
        
        assertTrue(error is AppError.ValidationError.TooShortError)
        assertEquals("Password", error.fieldName)
        assertEquals(8, error.minLength)
        assertEquals("Password must be at least 8 characters", error.message)
    }
    
    @Test
    fun testValidationErrorInvalidCharactersError() {
        val error = AppError.ValidationError.InvalidCharactersError("Username")
        
        assertTrue(error is AppError.ValidationError.InvalidCharactersError)
        assertEquals("Username", error.fieldName)
        assertEquals("Username contains invalid characters", error.message)
    }
    
    @Test
    fun testPlatformErrorStorageError() {
        val error = AppError.PlatformError.StorageError()
        
        assertTrue(error is AppError.PlatformError.StorageError)
        assertEquals("Failed to access device storage", error.message)
    }
    
    @Test
    fun testPlatformErrorPermissionError() {
        val error = AppError.PlatformError.PermissionError("CAMERA")
        
        assertTrue(error is AppError.PlatformError.PermissionError)
        assertEquals("CAMERA", error.permission)
        assertEquals("Permission required: CAMERA", error.message)
    }
    
    @Test
    fun testPlatformErrorNotificationError() {
        val error = AppError.PlatformError.NotificationError()
        
        assertTrue(error is AppError.PlatformError.NotificationError)
        assertEquals("Failed to handle notifications", error.message)
    }
    
    @Test
    fun testBusinessErrorMessageSendError() {
        val error = AppError.BusinessError.MessageSendError()
        
        assertTrue(error is AppError.BusinessError.MessageSendError)
        assertEquals("Failed to send message. Please try again.", error.message)
    }
    
    @Test
    fun testBusinessErrorConversationLoadError() {
        val error = AppError.BusinessError.ConversationLoadError()
        
        assertTrue(error is AppError.BusinessError.ConversationLoadError)
        assertEquals("Failed to load conversation history", error.message)
    }
    
    @Test
    fun testBusinessErrorSettingsSaveError() {
        val error = AppError.BusinessError.SettingsSaveError()
        
        assertTrue(error is AppError.BusinessError.SettingsSaveError)
        assertEquals("Failed to save settings", error.message)
    }
    
    @Test
    fun testBusinessErrorAuthenticationError() {
        val error = AppError.BusinessError.AuthenticationError()
        
        assertTrue(error is AppError.BusinessError.AuthenticationError)
        assertEquals("Authentication failed", error.message)
    }
    
    @Test
    fun testUnknownError() {
        val error = AppError.UnknownError()
        
        assertTrue(error is AppError.UnknownError)
        assertEquals("An unexpected error occurred", error.message)
    }
    
    @Test
    fun testUnknownErrorWithCustomMessage() {
        val customMessage = "Custom unknown error"
        val cause = Exception("Test cause")
        val error = AppError.UnknownError(customMessage, cause)
        
        assertEquals(customMessage, error.message)
        assertEquals(cause, error.cause)
    }
    
    @Test
    fun testThrowableToAppErrorNetworkException() {
        val exception = Exception("network connection failed")
        val appError = exception.toAppError()
        
        assertTrue(appError is AppError.NetworkError.ConnectionError)
        assertEquals("network connection failed", appError.message)
        assertEquals(exception, appError.cause)
    }
    
    @Test
    fun testThrowableToAppErrorTimeoutException() {
        val exception = Exception("timeout occurred")
        val appError = exception.toAppError()
        
        assertTrue(appError is AppError.NetworkError.TimeoutError)
        assertEquals("timeout occurred", appError.message)
        assertEquals(exception, appError.cause)
    }
    
    @Test
    fun testThrowableToAppErrorGenericException() {
        val exception = Exception("generic error")
        val appError = exception.toAppError()
        
        assertTrue(appError is AppError.UnknownError)
        assertEquals("generic error", appError.message)
        assertEquals(exception, appError.cause)
    }
    
    @Test
    fun testResultSuccess() {
        val result = Result.Success("test data")
        
        assertTrue(result is Result.Success)
        assertEquals("test data", result.data)
    }
    
    @Test
    fun testResultError() {
        val error = AppError.UnknownError("test error")
        val result = Result.Error(error)
        
        assertTrue(result is Result.Error)
        assertEquals(error, result.error)
    }
    
    @Test
    fun testResultMapSuccess() {
        val result = Result.Success(5)
        val mappedResult = result.map { it * 2 }
        
        assertTrue(mappedResult is Result.Success)
        assertEquals(10, (mappedResult as Result.Success).data)
    }
    
    @Test
    fun testResultMapError() {
        val error = AppError.UnknownError("test error")
        val result = Result.Error(error)
        val mappedResult = result.map { "transformed" }
        
        assertTrue(mappedResult is Result.Error)
        assertEquals(error, (mappedResult as Result.Error).error)
    }
    
    @Test
    fun testResultOnSuccess() {
        var actionCalled = false
        var receivedData: String? = null
        
        val result = Result.Success("test data")
        result.onSuccess { data ->
            actionCalled = true
            receivedData = data
        }
        
        assertTrue(actionCalled)
        assertEquals("test data", receivedData)
    }
    
    @Test
    fun testResultOnSuccessWithError() {
        var actionCalled = false
        
        val error = AppError.UnknownError("test error")
        val result = Result.Error(error)
        result.onSuccess { 
            actionCalled = true
        }
        
        assertFalse(actionCalled)
    }
    
    @Test
    fun testResultOnError() {
        var actionCalled = false
        var receivedError: AppError? = null
        
        val error = AppError.UnknownError("test error")
        val result = Result.Error(error)
        result.onError { err ->
            actionCalled = true
            receivedError = err
        }
        
        assertTrue(actionCalled)
        assertEquals(error, receivedError)
    }
    
    @Test
    fun testResultOnErrorWithSuccess() {
        var actionCalled = false
        
        val result = Result.Success("test data")
        result.onError { 
            actionCalled = true
        }
        
        assertFalse(actionCalled)
    }
    
    @Test
    fun testResultChaining() {
        var successCalled = false
        var errorCalled = false
        
        val result = Result.Success("test")
            .onSuccess { successCalled = true }
            .onError { errorCalled = true }
        
        assertTrue(successCalled)
        assertFalse(errorCalled)
        assertTrue(result is Result.Success)
    }
}