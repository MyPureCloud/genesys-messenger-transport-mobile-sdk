package com.genesys.cloud.messenger.composeapp.viewmodel

import com.genesys.cloud.messenger.composeapp.model.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.
 * Manages navigation and basic app state for the home screen with error handling.
 */
class HomeViewModel : BaseViewModel() {
    
    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()
    
    /**
     * Navigate to the interaction screen with error handling
     */
    fun navigateToInteraction() {
        scope.launch {
            safeExecuteUnit(showLoading = false) {
                _navigationEvent.value = NavigationEvent.NavigateToScreen(Screen.Interaction)
            }
        }
    }
    
    /**
     * Navigate to the settings screen with error handling
     */
    fun navigateToSettings() {
        scope.launch {
            safeExecuteUnit(showLoading = false) {
                _navigationEvent.value = NavigationEvent.NavigateToScreen(Screen.Settings)
            }
        }
    }
    
    /**
     * Clear the navigation event after it has been consumed
     */
    fun clearNavigationEvent() {
        _navigationEvent.value = null
    }
}

// HomeUiState is no longer needed as BaseViewModel handles loading and error states

/**
 * Navigation events from the Home screen
 */
sealed class NavigationEvent {
    data class NavigateToScreen(val screen: Screen) : NavigationEvent()
}