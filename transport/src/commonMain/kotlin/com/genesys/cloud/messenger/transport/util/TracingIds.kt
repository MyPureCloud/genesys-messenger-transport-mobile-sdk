package com.genesys.cloud.messenger.transport.util

internal object TracingIds {
    fun newId(): String = Platform().randomUUID()
}
