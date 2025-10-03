package com.genesys.cloud.messenger.transport.util.logs

internal expect class Logger(enableLogs: Boolean, tag: String) {
    fun d(message: () -> String)

    fun i(message: () -> String)

    fun w(message: () -> String)

    fun e(message: () -> String)

    fun e(
        throwable: Throwable?,
        message: () -> String
    )
}
