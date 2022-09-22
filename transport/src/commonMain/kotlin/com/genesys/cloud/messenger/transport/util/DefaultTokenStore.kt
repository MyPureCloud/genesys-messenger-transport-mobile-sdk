package com.genesys.cloud.messenger.transport.util

internal const val TOKEN_KEY = "token"

/**
 * A default implementation of the [TokenStore] that should be sufficient for most users of the
 * SDK.
 */
expect class DefaultTokenStore(storeKey: String) : TokenStore
