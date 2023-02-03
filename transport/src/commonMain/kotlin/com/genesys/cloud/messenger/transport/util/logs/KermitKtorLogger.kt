package com.genesys.cloud.messenger.transport.util.logs

import io.ktor.client.plugins.logging.Logger

internal class KermitKtorLogger(
    private val kermit: co.touchlab.kermit.Logger
) : Logger {
    override fun log(message: String) {
        kermit.i { message }
    }
}
