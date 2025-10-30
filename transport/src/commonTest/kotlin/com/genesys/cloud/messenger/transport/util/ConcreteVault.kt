package com.genesys.cloud.messenger.transport.util

/**
 * A concrete implementation of [Vault] for testing purposes.
 *
 * This class uses in-memory storage to simulate a real vault,
 * allowing easy manipulation and inspection of stored values
 * during unit tests.
 *
 * @property keys The set of keys used to access stored data.
 */
class ConcreteVault(
    keys: Keys =
        Keys(
            vaultKey = VAULT_KEY,
            tokenKey = TOKEN_KEY,
            authRefreshTokenKey = AUTH_REFRESH_TOKEN_KEY,
            wasAuthenticated = WAS_AUTHENTICATED,
            pushConfigKey = PUSH_CONFIG_KEY,
        ),
) : Vault(keys) {
    private val storage = mutableMapOf<String, String>()

    override fun store(
        key: String,
        value: String
    ) {
        storage[key] = value
    }

    override fun fetch(key: String): String? {
        return storage[key]
    }

    override fun remove(key: String) {
        storage.remove(key)
    }
}
