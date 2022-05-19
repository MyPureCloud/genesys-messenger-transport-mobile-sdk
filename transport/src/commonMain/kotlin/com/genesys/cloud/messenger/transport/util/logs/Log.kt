package com.genesys.cloud.messenger.transport.util.logs

import io.ktor.client.plugins.logging.Logger

internal expect class Log(enableLogs: Boolean, tag: String) {

    val ktorLogger: Logger

    fun withTag(tag: String): Log

    fun i(message: () -> String)

    fun w(message: () -> String)

    fun e(message: () -> String)

    fun e(throwable: Throwable, message: () -> String)
}
