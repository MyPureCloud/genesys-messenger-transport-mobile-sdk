package com.genesys.cloud.messenger.transport.util.logs

import co.touchlab.kermit.Severity
import io.ktor.client.plugins.logging.Logger

internal class Log(private val enableLogs: Boolean, tag: String) {

    val kermit = co.touchlab.kermit.Logger.withTag(tag)

    init {
        if (!enableLogs) {
            co.touchlab.kermit.Logger.setMinSeverity(Severity.Assert)
        }
    }

    val ktorLogger: Logger = KermitKtorLogger(
        co.touchlab.kermit.Logger.withTag(LogTag.API)
    )

    fun i(message: () -> String) = kermit.i(message)

    fun w(message: () -> String) = kermit.w(message)

    fun e(message: () -> String) = kermit.e(message)

    fun e(throwable: Throwable, message: () -> String) = kermit.e(throwable, message)

    fun withTag(tag: String) = Log(enableLogs, tag)
}
