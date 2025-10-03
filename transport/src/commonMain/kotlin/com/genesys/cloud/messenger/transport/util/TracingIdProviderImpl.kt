package com.genesys.cloud.messenger.transport.util

internal class TracingIdProviderImpl : TracingIdProvider {
    override fun getTracingId(): String = Platform().randomUUID()
}
