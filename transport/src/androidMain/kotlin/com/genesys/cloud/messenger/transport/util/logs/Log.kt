package com.genesys.cloud.messenger.transport.util.logs

import co.touchlab.kermit.Kermit
import co.touchlab.kermit.LogcatLogger
import co.touchlab.kermit.Severity
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.MessageLengthLimitingLogger
import okhttp3.logging.HttpLoggingInterceptor

internal actual class Log actual constructor(
    private val enableLogs: Boolean,
    tag: String
) {

    private val kermit: Kermit = Kermit(
        loggerList = listOf(
            ConfigurableKermitLogger(
                enabled = enableLogs,
                delegate = LogcatLogger(),
            )
        ),
        defaultTag = tag
    )

    actual val ktorLogger: Logger = MessageLengthLimitingLogger(
        delegate = KermitKtorLogger(
            kermit.withTag(LogTag.API)
        )
    )

    actual fun i(message: () -> String) = kermit.i(message)

    actual fun w(message: () -> String) = kermit.w(message)

    actual fun e(message: () -> String) = kermit.e(message)

    actual fun e(throwable: Throwable, message: () -> String) = kermit.e(throwable, message)

    actual fun withTag(tag: String): Log = Log(enableLogs = enableLogs, tag = tag)

    internal fun okHttpLogger(): HttpLoggingInterceptor.Logger =
        HttpLoggingInterceptor.Logger {
            message -> kermit.log(Severity.Info, LogTag.OKHTTP, null, message)
        }
}
