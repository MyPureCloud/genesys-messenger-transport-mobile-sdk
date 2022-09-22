package com.genesys.cloud.messenger.transport.util

internal object TokenGenerator {
    fun generate(): String = Platform().randomUUID()
}
