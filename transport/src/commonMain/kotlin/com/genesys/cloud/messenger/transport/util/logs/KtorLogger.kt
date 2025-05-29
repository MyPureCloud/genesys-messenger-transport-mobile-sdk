package com.genesys.cloud.messenger.transport.util.logs

import io.ktor.client.plugins.logging.Logger

internal class KtorLogger(
    private val logger: com.genesys.cloud.messenger.transport.util.logs.Logger
) : Logger {
    override fun log(message: String) {
        logger.i { message }
    }
}
