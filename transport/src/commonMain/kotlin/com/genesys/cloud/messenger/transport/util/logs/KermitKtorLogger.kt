package com.genesys.cloud.messenger.transport.util.logs

import co.touchlab.kermit.Kermit
import io.ktor.client.plugins.logging.Logger

internal class KermitKtorLogger(
    private val kermit: Kermit
) : Logger {
    override fun log(message: String) {
        kermit.i { message }
    }
}
