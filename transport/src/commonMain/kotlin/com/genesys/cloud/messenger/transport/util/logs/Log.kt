package com.genesys.cloud.messenger.transport.util.logs

internal class Log(
    private val enableLogs: Boolean,
    tag: String,
    val logger: Logger = Logger(enableLogs, tag)
) {

    val ktorLogger: io.ktor.client.plugins.logging.Logger = KtorLogger(
        Logger(enableLogs, LogTag.API)
    )

    fun i(message: () -> String) = logger.i(message)

    fun w(message: () -> String) = logger.w(message)

    fun e(message: () -> String) = logger.e(message)

    fun e(throwable: Throwable, message: () -> String) = logger.e(throwable, message)

    fun withTag(tag: String) = Log(enableLogs, tag)
}
