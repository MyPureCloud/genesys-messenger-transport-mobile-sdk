package com.genesys.cloud.messenger.journey.util.logs

import android.util.Log

internal actual class Logger actual constructor(
    private val enableLogs: Boolean,
    val tag: String,
) {
    actual fun i(message: () -> String) {
        if (enableLogs) Log.i(tag, message())
    }

    actual fun w(message: () -> String) {
        if (enableLogs) Log.w(tag, message())
    }

    actual fun e(message: () -> String) {
        if (enableLogs) Log.e(tag, message())
    }

    actual fun e(throwable: Throwable?, message: () -> String) {
        if (enableLogs) Log.e(tag, message(), throwable)
    }
}
