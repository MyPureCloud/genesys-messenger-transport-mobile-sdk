package com.genesys.cloud.messenger.transport.util

import com.genesys.cloud.messenger.transport.core.Vault

actual class DefaultTokenStore actual constructor(storeKey: String) : TokenStore() {
    private val store = Vault(storeKey)

    override fun store(token: String) {
        store.set(TOKEN_KEY, token)
    }

    override fun fetch(): String? {
        return store.string(TOKEN_KEY)
    }

    override fun storeAuthRefreshToken(refreshToken: String) {
        store.set(AUTH_REFRESH_TOKEN_KEY, refreshToken)
    }

    override fun fetchAuthRefreshToken(): String? {
        return store.string(AUTH_REFRESH_TOKEN_KEY)
    }
}
