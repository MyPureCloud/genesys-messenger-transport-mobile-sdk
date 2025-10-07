package com.genesys.cloud.messenger.transport.util

object TracingIdProvider {
    fun getTracingId(): String = Platform().randomUUID()
}
