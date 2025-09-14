package com.genesys.cloud.messenger.composeapp.viewmodel

import com.genesys.cloud.messenger.composeapp.model.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.
 * Manages navigation and basic app state for the home screen.
 */
class HomeViewModel : BaseViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()
    
    /**
     * Navigate to the chat screen
     */
    fun navigateToChat() {
        scope.launch {
            _navigationEvent.value = NavigationEvent.NavigateToScreen(Screen.Chat)
        }
    }
    
    /**
     * Navigate to the settings screen
     */
    fun navigateToSettings() {
        scope.launch {
            _navigationEvent.value = NavigationEvent.NavigateToScreen(Screen.Settings)
        }
    }
    
    /**
     * Clear the navigation event after it has been consumed
     */
    fun clearNavigationEvent() {
        _navigationEvent.value = null
    }
    
    /**
     * Set loading state
     */
    fun setLoading(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = isLoading)
    }
    
    /**
     * Set error message
     */
    fun setError(error: String?) {
        _uiState.value = _uiState.value.copy(error = error)
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI state for the Home screen
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Navigation events from the Home screen
 */
sealed class NavigationEvent {
    data class NavigateToScreen(val screen: Screen) : NavigationEvent()
}