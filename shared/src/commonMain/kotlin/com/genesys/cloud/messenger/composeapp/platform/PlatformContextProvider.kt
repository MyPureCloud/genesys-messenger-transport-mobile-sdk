package com.genesys.cloud.messenger.composeapp.platform

import com.genesys.cloud.messenger.composeapp.model.PlatformContext

/**
 * Platform-specific context provider for TestBedViewModel initialization.
 * This provides a clean interface for getting platform context without exposing
 * platform-specific details to the common code.
 */
expect object PlatformContextProvider {
    
    /**
     * Get the current platform context.
     * On Android, this requires an Android Context to be set first.
     * On iOS, this can be called directly.
     */
    fun getCurrentPlatformContext(): PlatformContext?
    
    /**
     * Check if platform context is available
     */
    fun isPlatformContextAvailable(): Boolean
}