package com.genesys.cloud.messenger.composeapp.platform



/**
 * iOS-specific platform provider
 */
object IosPlatformProvider {
    
    /**
     * Create a PlatformManager instance for iOS
     */
    fun createPlatformManager(): PlatformManager {
        // For iOS, we use a simple object as context
        val context = Any()
        return PlatformManager(IosPlatformContext(context))
    }
    
    /**
     * Get platform operations for iOS
     */
    fun getPlatformOperations(): PlatformOperations {
        val context = Any()
        return PlatformOperations(IosPlatformContext(context))
    }
}