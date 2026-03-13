package com.genesys.cloud.messenger.journey.util.logs

import io.ktor.client.plugins.logging.Logger as KtorLoggerInterface

internal class Log(
    private val enableLogs: Boolean,
    tag: String,
    val logger: Logger = Logger(enableLogs, tag),
) {

    val ktorLogger: KtorLoggerInterface = object : KtorLoggerInterface {
        private val ktLogger = Logger(enableLogs, LogTag.HTTP_CLIENT)

        override fun log(message: String) {
            ktLogger.i { message }
        }
    }

    fun i(message: () -> String) = logger.i(message)

    fun w(message: () -> String) = logger.w(message)

    fun e(message: () -> String) = logger.e(message)

    fun e(throwable: Throwable, message: () -> String) = logger.e(throwable, message)

    fun withTag(tag: String) = Log(enableLogs, tag)
}
