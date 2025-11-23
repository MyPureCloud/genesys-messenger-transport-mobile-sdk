package com.genesys.cloud.messenger.transport.util

import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Android implementation to check if an exception is caused by network connectivity issues.
 * Checks for specific network-related exception types.
 */
internal actual fun Exception.isNetworkException(): Boolean {
    return when (this) {
        is UnknownHostException -> true
        is ConnectException -> true
        is SocketTimeoutException -> true
        is SocketException -> true
        else -> {
            // Check the cause chain
            val cause = this.cause
            cause is UnknownHostException ||
                cause is ConnectException ||
                cause is SocketTimeoutException ||
                cause is SocketException
        }
    }
}

