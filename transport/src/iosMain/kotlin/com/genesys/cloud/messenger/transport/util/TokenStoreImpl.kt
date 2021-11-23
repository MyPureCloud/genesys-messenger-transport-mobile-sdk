package com.genesys.cloud.messenger.transport.util

import com.genesys.cloud.messenger.transport.util.logs.Log
import platform.Foundation.NSUUID

internal class TokenStoreImpl(storeKey: String, private val log: Log) : TokenStore {

    private val store = Vault(storeKey)
    override val token: String
        get() = store.string(TOKEN_KEY) ?: NSUUID().UUIDString().also {
            if (!store.set(TOKEN_KEY, it)) {
                log.e { "Failed to store token: $it" }
            }
        }
}
