package com.genesys.cloud.messenger.composeapp.viewmodel

import com.genesys.cloud.messenger.composeapp.config.AppConfig
import com.genesys.cloud.messenger.composeapp.model.AppSettings
import com.genesys.cloud.messenger.composeapp.model.Result
import com.genesys.cloud.messenger.composeapp.validation.InputValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Simplified ViewModel for the Settings screen.
 * Manages only deployment configuration (deploymentId and region).
 */
class SettingsViewModel : BaseViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    private val _settings = MutableStateFlow(
        AppSettings(
            deploymentId = AppConfig.DEFAULT_DEPLOYMENT_ID,
            region = AppConfig.DEFAULT_REGION
        )
    )
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    
    init {
        loadDefaultSettings()
    }
    
    /**
     * Update the deployment ID with validation
     */
    fun updateDeploymentId(deploymentId: String) {
        scope.launch {
            safeExecuteUnit(showLoading = false) {
                val validationResult = InputValidator.validateDeploymentId(deploymentId)
                
                when (validationResult) {
                    is Result.Success -> {
                        val updatedSettings = _settings.value.copy(deploymentId = validationResult.data)
                        _settings.value = updatedSettings
                        saveSettings(updatedSettings)
                        showSuccessMessage("Deployment ID updated successfully")
                    }
                    is Result.Error -> {
                        handleError(validationResult.error)
                    }
                }
            }
        }
    }
    
    /**
     * Update the region with validation
     */
    fun updateRegion(region: String) {
        scope.launch {
            safeExecuteUnit(showLoading = false) {
                val validationResult = InputValidator.validateRegion(region, getAvailableRegions())
                
                when (validationResult) {
                    is Result.Success -> {
                        val updatedSettings = _settings.value.copy(region = validationResult.data)
                        _settings.value = updatedSettings
                        saveSettings(updatedSettings)
                        showSuccessMessage("Region updated successfully")
                    }
                    is Result.Error -> {
                        handleError(validationResult.error)
                    }
                }
            }
        }
    }
    
    /**
     * Reset settings to default values from AppConfig
     */
    fun resetToDefaults() {
        scope.launch {
            safeExecuteUnit {
                // Simulate reset operation
                delay(300)
                
                // Simulate potential error (for demonstration)
                if (Random.nextFloat() < 0.001f) { // 0.1% chance of error
                    throw Exception("Failed to reset settings")
                }
                
                val defaultSettings = AppSettings(
                    deploymentId = AppConfig.DEFAULT_DEPLOYMENT_ID,
                    region = AppConfig.DEFAULT_REGION
                )
                _settings.value = defaultSettings
                saveSettings(defaultSettings)
                showSuccessMessage("Settings reset to defaults")
            }
        }
    }
    
    /**
     * Load default settings from AppConfig (BuildConfig equivalent)
     */
    private fun loadDefaultSettings() {
        scope.launch {
            safeExecuteUnit {
                // Simulate loading from storage
                delay(200)
                
                // Simulate potential loading error (for demonstration)
                if (Random.nextFloat() < 0.001f) { // 0.1% chance of error
                    throw Exception("Failed to access settings storage")
                }
                
                // Load from storage or use defaults if empty
                val loadedSettings = loadSettingsFromStorage() ?: AppSettings(
                    deploymentId = AppConfig.DEFAULT_DEPLOYMENT_ID,
                    region = AppConfig.DEFAULT_REGION
                )
                
                _settings.value = loadedSettings
            }
        }
    }
    
    /**
     * Reload settings from storage
     */
    fun reloadSettings() {
        loadDefaultSettings()
    }
    
    /**
     * Save settings to storage with error handling
     */
    private suspend fun saveSettings(@Suppress("UNUSED_PARAMETER") settings: AppSettings) {
        // Simulate saving to storage
        delay(150)
        
        // Simulate potential save error (for demonstration)
        if (Random.nextFloat() < 0.001f) { // 0.1% chance of error
            throw Exception("Failed to save settings to storage")
        }
        
        // In a real implementation, this would save to platform-specific storage
        // Success - no action needed as success message is handled by caller
    }
    
    /**
     * Load settings from platform-specific storage
     * Returns null if no settings are stored (first run)
     */
    private suspend fun loadSettingsFromStorage(): AppSettings? {
        // Simulate loading from storage
        delay(100)
        
        // In a real implementation, this would load from platform-specific storage
        // For now, return null to indicate no stored settings (use defaults)
        return null
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
     * Get available regions for deployment configuration
     */
    fun getAvailableRegions(): List<String> {
        return AppConfig.AVAILABLE_REGIONS
    }
    
    /**
     * Get default deployment ID from AppConfig
     */
    fun getDefaultDeploymentId(): String {
        return AppConfig.DEFAULT_DEPLOYMENT_ID
    }
    
    /**
     * Get default region from AppConfig
     */
    fun getDefaultRegion(): String {
        return AppConfig.DEFAULT_REGION
    }
}

/**
 * UI state for the simplified Settings screen
 */
data class SettingsUiState(
    val successMessage: String? = null
)