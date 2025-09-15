package com.genesys.cloud.messenger.composeapp.util

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.timeIntervalSince1970

/**
 * iOS implementation of time utilities
 */
actual fun getCurrentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

/**
 * Format timestamp for display in HH:mm:ss format
 */
actual fun formatTimestamp(timestamp: Long): String {
    val date = NSDate(timestamp / 1000.0)
    val formatter = NSDateFormatter()
    formatter.dateFormat = "HH:mm:ss"
    return formatter.stringFromDate(date)
}