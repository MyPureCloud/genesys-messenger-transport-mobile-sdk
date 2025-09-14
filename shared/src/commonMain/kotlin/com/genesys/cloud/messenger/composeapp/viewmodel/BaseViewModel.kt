package com.genesys.cloud.messenger.composeapp.viewmodel

import com.genesys.cloud.messenger.composeapp.model.AppError
import com.genesys.cloud.messenger.composeapp.model.Result
import com.genesys.cloud.messenger.composeapp.model.toAppError
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Base class for ViewModels in the multiplatform project.
 * Provides a coroutine scope for managing asynchronous operations and error handling.
 */
abstract class BaseViewModel {
    
    // Error handling
    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * Exception handler for coroutines that converts exceptions to AppError
     */
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        handleError(exception.toAppError())
    }
    
    /**
     * Coroutine scope for this ViewModel.
     * Uses SupervisorJob to ensure that failure of one coroutine doesn't cancel others.
     * Includes exception handler for automatic error handling.
     */
    protected val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main + exceptionHandler
    )
    
    /**
     * Handle an error by updating the error state
     */
    protected fun handleError(error: AppError) {
        _error.value = error
        _isLoading.value = false
    }
    
    /**
     * Handle an exception by converting it to AppError
     */
    protected fun handleException(exception: Throwable) {
        handleError(exception.toAppError())
    }
    
    /**
     * Clear the current error
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Set loading state
     */
    protected fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
    
    /**
     * Execute a suspending operation with automatic error handling and loading state
     */
    protected suspend fun <T> safeExecute(
        showLoading: Boolean = true,
        operation: suspend () -> Result<T>
    ): Result<T> {
        return try {
            if (showLoading) setLoading(true)
            clearError()
            
            val result = operation()
            
            when (result) {
                is Result.Error -> {
                    handleError(result.error)
                }
                is Result.Success -> {
                    // Success - no error handling needed
                }
            }
            
            result
        } catch (exception: Throwable) {
            val error = exception.toAppError()
            handleError(error)
            Result.Error(error)
        } finally {
            if (showLoading) setLoading(false)
        }
    }
    
    /**
     * Execute a suspending operation that doesn't return a Result
     */
    protected suspend fun safeExecuteUnit(
        showLoading: Boolean = true,
        operation: suspend () -> Unit
    ) {
        try {
            if (showLoading) setLoading(true)
            clearError()
            operation()
        } catch (exception: Throwable) {
            handleError(exception.toAppError())
        } finally {
            if (showLoading) setLoading(false)
        }
    }
    
    /**
     * Called when the ViewModel is no longer needed.
     * Cancels all ongoing coroutines.
     */
    open fun onCleared() {
        scope.cancel()
    }
}