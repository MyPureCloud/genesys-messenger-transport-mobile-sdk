package com.genesys.cloud.messenger.transport.util

import com.liftric.kvault.KVault
import platform.Foundation.NSUUID

internal class TokenStoreImpl(storeKey: String) : TokenStore {

    private val store = KVault(storeKey)
    override val token: String
        get() = store.string(TOKEN_KEY) ?: NSUUID().UUIDString().also {
            store.set(TOKEN_KEY, it)
        }
}
