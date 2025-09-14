package com.genesys.cloud.messenger.composeapp.platform

/**
 * Platform-specific context for operations that require platform access
 */
expect interface PlatformContext

/**
 * Platform-specific operations interface
 */
expect class PlatformOperations(context: PlatformContext) {
    
    /**
     * Show a platform-specific toast/alert message
     */
    fun showToast(message: String)
    
    /**
     * Get device information
     */
    fun getDeviceInfo(): DeviceInfo
    
    /**
     * Open a URL in the platform's default browser
     */
    fun openUrl(url: String)
    
    /**
     * Get platform-specific storage directory
     */
    fun getStorageDirectory(): String
    
    /**
     * Check if network is available
     */
    fun isNetworkAvailable(): Boolean
}

/**
 * Device information data class
 */
data class DeviceInfo(
    val model: String,
    val osVersion: String,
    val appVersion: String,
    val deviceId: String
)