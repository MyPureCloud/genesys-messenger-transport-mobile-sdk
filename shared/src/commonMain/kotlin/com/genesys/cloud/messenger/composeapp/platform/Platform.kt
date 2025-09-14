package com.genesys.cloud.messenger.composeapp.platform

/**
 * Platform-specific information and capabilities
 */
expect class Platform() {
    val name: String
    val version: String
    val isDebug: Boolean
}

/**
 * Get the current platform instance
 */
expect fun getPlatform(): Platform