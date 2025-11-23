package com.genesys.cloud.messenger.transport.util

import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

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
