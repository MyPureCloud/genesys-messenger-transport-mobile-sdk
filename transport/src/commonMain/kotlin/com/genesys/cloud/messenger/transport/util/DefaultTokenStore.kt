package com.genesys.cloud.messenger.transport.util

/**
 * A default implementation of the [TokenStore] that should be sufficient for most users of the
 * SDK.
 */
expect class DefaultTokenStore(storeKey: String) : TokenStore {
    override fun store(token: String)

    override fun fetch(): String?
}
