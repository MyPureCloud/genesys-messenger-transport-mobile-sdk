package com.genesys.cloud.messenger.composeapp.platform

/**
 * Platform manager to handle platform-specific operations
 */
class PlatformManager(private val context: PlatformContext) {
    
    private val operations = PlatformOperations(context)
    private val platform = getPlatform()
    
    /**
     * Get platform information
     */
    fun getPlatformInfo(): Platform = platform
    
    /**
     * Show a toast/alert message
     */
    fun showMessage(message: String) {
        operations.showToast(message)
    }
    
    /**
     * Get device information
     */
    fun getDeviceInfo(): DeviceInfo = operations.getDeviceInfo()
    
    /**
     * Open URL in default browser
     */
    fun openUrl(url: String) {
        operations.openUrl(url)
    }
    
    /**
     * Get storage directory path
     */
    fun getStorageDirectory(): String = operations.getStorageDirectory()
    
    /**
     * Check network availability
     */
    fun isNetworkAvailable(): Boolean = operations.isNetworkAvailable()
    
    /**
     * Get platform name for display
     */
    fun getPlatformDisplayName(): String = "${platform.name} ${platform.version}"
}