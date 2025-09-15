package com.genesys.cloud.messenger.composeapp.util

/**
 * Get current time in milliseconds
 */
expect fun getCurrentTimeMillis(): Long

/**
 * Format timestamp for display
 */
expect fun formatTimestamp(timestamp: Long): String