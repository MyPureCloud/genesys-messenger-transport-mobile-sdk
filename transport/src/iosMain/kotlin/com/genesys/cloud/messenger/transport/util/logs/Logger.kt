package com.genesys.cloud.messenger.transport.util.logs

import platform.Foundation.NSLog

internal actual class Logger actual constructor(
    private val enableLogs: Boolean,
    val tag: String
) {

    actual fun d(message: () -> String) {
        if (enableLogs) NSLog("DEBUG: [$tag] ${message()}")
    }

    actual fun i(message: () -> String) {
        if (enableLogs) NSLog("INFO: [$tag] ${message()}")
    }

    actual fun w(message: () -> String) {
        if (enableLogs) NSLog("WARNING: [$tag] ${message()}")
    }

    actual fun e(message: () -> String) {
        if (enableLogs) NSLog("ERROR: ${message()}")
    }

    actual fun e(throwable: Throwable?, message: () -> String) {
        if (enableLogs) {
            NSLog("ERROR: [$tag] ${message()}")
            throwable?.let { NSLog("ERROR: ${it.message}") }
        }
    }
}
