package com.genesys.cloud.messenger.composeapp.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Base class for ViewModels in the multiplatform project.
 * Provides a coroutine scope for managing asynchronous operations.
 */
abstract class BaseViewModel {
    
    /**
     * Coroutine scope for this ViewModel.
     * Uses SupervisorJob to ensure that failure of one coroutine doesn't cancel others.
     */
    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    /**
     * Called when the ViewModel is no longer needed.
     * Cancels all ongoing coroutines.
     */
    open fun onCleared() {
        scope.cancel()
    }
}