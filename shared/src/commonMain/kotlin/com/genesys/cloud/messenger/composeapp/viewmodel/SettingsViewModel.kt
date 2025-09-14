package com.genesys.cloud.messenger.composeapp.viewmodel

import com.genesys.cloud.messenger.composeapp.model.AppSettings
import com.genesys.cloud.messenger.composeapp.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * ViewModel for the Settings screen.
 * Manages app preferences and configuration settings.
 */
class SettingsViewModel : BaseViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    
    init {
        loadSettings()
    }
    
    /**
     * Update the theme mode setting
     */
    fun updateThemeMode(themeMode: ThemeMode) {
        scope.launch {
            val updatedSettings = _settings.value.copy(theme = themeMode)
            _settings.value = updatedSettings
            saveSettings(updatedSettings)
        }
    }
    
    /**
     * Toggle notifications setting
     */
    fun toggleNotifications() {
        scope.launch {
            val updatedSettings = _settings.value.copy(notifications = !_settings.value.notifications)
            _settings.value = updatedSettings
            saveSettings(updatedSettings)
        }
    }
    
    /**
     * Update the language setting
     */
    fun updateLanguage(language: String) {
        scope.launch {
            val updatedSettings = _settings.value.copy(language = language)
            _settings.value = updatedSettings
            saveSettings(updatedSettings)
        }
    }
    
    /**
     * Reset all settings to default values
     */
    fun resetToDefaults() {
        scope.launch {
            setLoading(true)
            
            // Simulate reset operation
            delay(500)
            
            val defaultSettings = AppSettings()
            _settings.value = defaultSettings
            saveSettings(defaultSettings)
            
            setLoading(false)
            showSuccessMessage("Settings reset to defaults")
        }
    }
    
    /**
     * Load settings from storage (placeholder implementation)
     */
    private fun loadSettings() {
        scope.launch {
            setLoading(true)
            
            try {
                // Simulate loading from storage
                delay(500)
                
                // In a real implementation, this would load from platform-specific storage
                // For now, we'll use default settings
                val loadedSettings = AppSettings(
                    theme = ThemeMode.System,
                    notifications = true,
                    language = "en"
                )
                
                _settings.value = loadedSettings
                setLoading(false)
            } catch (e: Exception) {
                setError("Failed to load settings: ${e.message}")
                setLoading(false)
            }
        }
    }
    
    /**
     * Save settings to storage (placeholder implementation)
     */
    private fun saveSettings(settings: AppSettings) {
        scope.launch {
            try {
                // Simulate saving to storage
                delay(200)
                
                // In a real implementation, this would save to platform-specific storage
                // For now, we'll just show a success message
                showSuccessMessage("Settings saved")
            } catch (e: Exception) {
                setError("Failed to save settings: ${e.message}")
            }
        }
    }
    
    /**
     * Set loading state
     */
    private fun setLoading(isLoading: Boolean) {
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
    
    /**
     * Show success message
     */
    private fun showSuccessMessage(message: String) {
        _uiState.value = _uiState.value.copy(successMessage = message)
        
        // Auto-clear success message after 3 seconds
        scope.launch {
            delay(3000)
            clearSuccessMessage()
        }
    }
    
    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
    
    /**
     * Get available theme modes
     */
    fun getAvailableThemeModes(): List<ThemeMode> {
        return listOf(ThemeMode.Light, ThemeMode.Dark, ThemeMode.System)
    }
    
    /**
     * Get available languages
     */
    fun getAvailableLanguages(): List<LanguageOption> {
        return listOf(
            LanguageOption("en", "English"),
            LanguageOption("es", "Español"),
            LanguageOption("fr", "Français"),
            LanguageOption("de", "Deutsch"),
            LanguageOption("it", "Italiano"),
            LanguageOption("pt", "Português")
        )
    }
}

/**
 * UI state for the Settings screen
 */
data class SettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

/**
 * Represents a language option in the settings
 */
data class LanguageOption(
    val code: String,
    val displayName: String
)