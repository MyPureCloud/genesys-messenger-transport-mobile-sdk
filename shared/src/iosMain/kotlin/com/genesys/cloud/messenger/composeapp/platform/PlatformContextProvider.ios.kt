package com.genesys.cloud.messenger.composeapp.platform

import com.genesys.cloud.messenger.composeapp.model.PlatformContext
import com.genesys.cloud.messenger.composeapp.model.getPlatformContext

/**
 * iOS implementation of PlatformContextProvider
 */
actual object PlatformContextProvider {
    
    /**
     * Get the current iOS platform context
     */
    actual fun getCurrentPlatformContext(): PlatformContext? {
        return getPlatformContext()
    }
    
    /**
     * Check if iOS context is available (always true on iOS)
     */
    actual fun isPlatformContextAvailable(): Boolean {
        return true
    }
}