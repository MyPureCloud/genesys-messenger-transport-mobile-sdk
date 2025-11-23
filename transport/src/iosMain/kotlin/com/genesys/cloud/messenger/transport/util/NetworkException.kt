package com.genesys.cloud.messenger.transport.util

import platform.Foundation.NSError
import platform.Foundation.NSURLErrorDomain

internal actual fun Exception.isNetworkException(): Boolean {
    val nsError =
        when {
            this is NSError -> this
            this.cause is NSError -> this.cause as NSError
            else -> null
        }

    if (nsError != null && nsError.domain == NSURLErrorDomain) {
        return when (nsError.code) {
            -1009L, // NSURLErrorNotConnectedToInternet
            -1004L, // NSURLErrorCannotConnectToHost
            -1003L, // NSURLErrorCannotFindHost
            -1005L, // NSURLErrorNetworkConnectionLost
            -1001L // NSURLErrorTimedOut
            -> true
            else -> false
        }
    }

    return false
}
