package com.genesys.cloud.messenger.composeapp.platform

/**
 * Example usage of platform-specific interfaces
 */
class PlatformExample(private val platformManager: PlatformManager) {
    
    /**
     * Display platform information
     */
    fun displayPlatformInfo(): String {
        val platform = platformManager.getPlatformInfo()
        return "Running on ${platform.name} ${platform.version}"
    }
    
    /**
     * Show a welcome message using platform-specific toast/alert
     */
    fun showWelcomeMessage() {
        val platformName = platformManager.getPlatformDisplayName()
        platformManager.showMessage("Welcome to Compose Multiplatform on $platformName!")
    }
    
    /**
     * Get device information as a formatted string
     */
    fun getDeviceInfoString(): String {
        val deviceInfo = platformManager.getDeviceInfo()
        return buildString {
            appendLine("Device: ${deviceInfo.model}")
            appendLine("OS: ${deviceInfo.osVersion}")
            appendLine("App Version: ${deviceInfo.appVersion}")
            appendLine("Device ID: ${deviceInfo.deviceId}")
            appendLine("Storage: ${platformManager.getStorageDirectory()}")
            appendLine("Network: ${if (platformManager.isNetworkAvailable()) "Available" else "Not Available"}")
        }
    }
    
    /**
     * Open a sample URL
     */
    fun openSampleUrl() {
        platformManager.openUrl("https://www.genesys.com")
    }
}