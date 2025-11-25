package com.genesys.cloud.messenger.transport.util

import io.ktor.client.engine.darwin.DarwinHttpRequestException

internal actual fun Exception.isNetworkException(): Boolean {
    return this is DarwinHttpRequestException
}
