package com.genesys.cloud.messenger.transport.util

interface TracingIdProvider {
    fun getTracingId(): String
}
