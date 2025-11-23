package com.genesys.cloud.messenger.transport.util

import platform.Foundation.NSError
import platform.Foundation.NSURLErrorDomain

/**
 * iOS implementation to check if an exception is caused by network connectivity issues.
 * Checks for NSError instances with NSURLErrorDomain and network-related error codes.
 */
internal actual fun Exception.isNetworkException(): Boolean {
    // Check if this exception or its cause is an NSError with network error codes
    val nsError = when {
        this is NSError -> this
        this.cause is NSError -> this.cause as NSError
        else -> null
    }
    
    if (nsError != null && nsError.domain == NSURLErrorDomain) {
        // Check for network-related error codes
        return when (nsError.code) {
            -1009L, // NSURLErrorNotConnectedToInternet
            -1004L, // NSURLErrorCannotConnectToHost
            -1003L, // NSURLErrorCannotFindHost
            -1005L, // NSURLErrorNetworkConnectionLost
            -1001L  // NSURLErrorTimedOut
            -> true
            else -> false
        }
    }
    
    return false
}


