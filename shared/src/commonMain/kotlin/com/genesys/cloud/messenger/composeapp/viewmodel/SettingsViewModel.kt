package com.genesys.cloud.messenger.composeapp.viewmodel

import com.genesys.cloud.messenger.composeapp.model.AppError
import com.genesys.cloud.messenger.composeapp.model.AppSettings
import com.genesys.cloud.messenger.composeapp.model.Result
import com.genesys.cloud.messenger.composeapp.model.ThemeMode
import com.genesys.cloud.messenger.composeapp.validation.InputValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * ViewModel for the Settings screen.
 * Manages app preferences, configuration settings, and validation.
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
     * Update the theme mode setting with validation
     */
    fun updateThemeMode(themeMode: ThemeMode) {
        scope.launch {
            safeExecuteUnit(showLoading = false) {
                val updatedSettings = _settings.value.copy(theme = themeMode)
                _settings.value = updatedSettings
                saveSettings(updatedSettings)
                showSuccessMessage("Theme updated successfully")
            }
        }
    }
    
    /**
     * Toggle notifications setting with validation
     */
    fun toggleNotifications() {
        scope.launch {
            safeExecuteUnit(showLoading = false) {
                val updatedSettings = _settings.value.copy(notifications = !_settings.value.notifications)
                _settings.value = updatedSettings
                saveSettings(updatedSettings)
                showSuccessMessage("Notification settings updated")
            }
        }
    }
    
    /**
     * Update the language setting with validation
     */
    fun updateLanguage(language: String) {
        scope.launch {
            safeExecuteUnit(showLoading = false) {
                // Validate language code
                val availableLanguages = getAvailableLanguages().map { it.code }
                val validationResult = InputValidator.validateLanguageCode(language, availableLanguages)
                
                when (validationResult) {
                    is Result.Success -> {
                        val updatedSettings = _settings.value.copy(language = validationResult.data)
                        _settings.value = updatedSettings
                        saveSettings(updatedSettings)
                        showSuccessMessage("Language updated successfully")
                    }
                    is Result.Error -> {
                        handleError(validationResult.error)
                    }
                }
            }
        }
    }
    
    /**
     * Reset all settings to default values with confirmation
     */
    fun resetToDefaults() {
        scope.launch {
            safeExecuteUnit {
                // Simulate reset operation
                delay(500)
                
                // Simulate potential error (for demonstration)
                if (Random.nextFloat() < 0.1f) { // 10% chance of error
                    throw Exception("Failed to reset settings")
                }
                
                val defaultSettings = AppSettings()
                _settings.value = defaultSettings
                saveSettings(defaultSettings)
                showSuccessMessage("Settings reset to defaults")
            }
        }
    }
    
    /**
     * Load settings from storage with error handling
     */
    private fun loadSettings() {
        scope.launch {
            safeExecuteUnit {
                // Simulate loading from storage
                delay(500)
                
                // Simulate potential loading error (for demonstration)
                if (Random.nextFloat() < 0.05f) { // 5% chance of error
                    throw Exception("Failed to access settings storage")
                }
                
                // In a real implementation, this would load from platform-specific storage
                val loadedSettings = AppSettings(
                    theme = ThemeMode.System,
                    notifications = true,
                    language = "en"
                )
                
                _settings.value = loadedSettings
            }
        }
    }
    
    /**
     * Reload settings from storage
     */
    fun reloadSettings() {
        loadSettings()
    }
    
    /**
     * Save settings to storage with error handling
     */
    private suspend fun saveSettings(settings: AppSettings) {
        // Simulate saving to storage
        delay(200)
        
        // Simulate potential save error (for demonstration)
        if (Random.nextFloat() < 0.05f) { // 5% chance of error
            throw Exception("Failed to save settings to storage")
        }
        
        // In a real implementation, this would save to platform-specific storage
        // Success - no action needed as success message is handled by caller
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
    val successMessage: String? = null
)

/**
 * Represents a language option in the settings
 */
data class LanguageOption(
    val code: String,
    val displayName: String
)