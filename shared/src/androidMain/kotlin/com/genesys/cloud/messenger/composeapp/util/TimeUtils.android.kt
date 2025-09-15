package com.genesys.cloud.messenger.composeapp.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Android implementation of time utilities
 */
actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

/**
 * Format timestamp for display in HH:mm:ss format
 */
actual fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(timestamp))
}