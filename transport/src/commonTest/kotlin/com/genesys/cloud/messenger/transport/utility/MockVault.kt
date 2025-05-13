package com.genesys.cloud.messenger.transport.utility

import com.genesys.cloud.messenger.transport.util.VAULT_KEY
import com.genesys.cloud.messenger.transport.util.TOKEN_KEY
import com.genesys.cloud.messenger.transport.util.AUTH_REFRESH_TOKEN_KEY
import com.genesys.cloud.messenger.transport.util.WAS_AUTHENTICATED
import com.genesys.cloud.messenger.transport.util.Vault

/**
 * A mock implementation of the Vault interface for testing purposes.
 * This implementation stores values in a simple in-memory map.
 */
class MockVault(
    keys: Keys = Keys(
        vaultKey = VAULT_KEY,
        tokenKey = TOKEN_KEY,
        authRefreshTokenKey = AUTH_REFRESH_TOKEN_KEY,
        wasAuthenticated = WAS_AUTHENTICATED,
    ),
) : Vault(keys) {
    private val storage = mutableMapOf<String, String>()

    override fun store(key: String, value: String) {
        storage[key] = value
    }

    override fun fetch(key: String): String? {
        return storage[key]
    }

    override fun remove(key: String) {
        storage.remove(key)
    }

    /**
     * Clears all stored values.
     */
    fun clear() {
        storage.clear()
    }
}