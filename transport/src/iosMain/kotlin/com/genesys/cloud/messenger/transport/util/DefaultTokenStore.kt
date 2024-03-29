package com.genesys.cloud.messenger.transport.util

import com.genesys.cloud.messenger.transport.core.InternalVault

@Deprecated("Use [Vault] instead.")
actual class DefaultTokenStore actual constructor(storeKey: String) : TokenStore() {
    private val store = InternalVault(storeKey)

    override fun store(token: String) {
        store.set(TOKEN_KEY, token)
    }

    override fun fetch(): String? {
        return store.string(TOKEN_KEY)
    }
}
