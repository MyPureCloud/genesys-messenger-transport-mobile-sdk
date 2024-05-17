package com.genesys.cloud.messenger.transport.util

import com.genesys.cloud.messenger.transport.auth.NO_REFRESH_TOKEN

/**
 * Abstract class representing a vault that stores and retrieves sensitive data using a provided set of keys.
 * Users of the library can use the [DefaultVault]
 * implementation for simplicity or create a custom vault by making a concrete implementation
 * of this class and passing it in to the [MessengerTransportSDK] constructor.
 *
 * @param keys The set of keys used to access stored data in the vault.
 */
abstract class Vault(val keys: Keys) {
    /**
     * Retrieves the token value from the vault or generates a new token if it doesn't exist.
     * The token is stored in the vault for future retrieval.
     */
    internal val token: String
        get() = fetch(keys.tokenKey) ?: TokenGenerator.generate().also {
            store(keys.tokenKey, it)
        }

    /**
     * Retrieves the auth refresh token value from the vault or returns a default value if it doesn't exist.
     * The auth refresh token can also be set to a new value, which will then be stored in the vault.
     */
    internal var authRefreshToken: String
        get() = fetch(keys.authRefreshTokenKey) ?: NO_REFRESH_TOKEN
        set(value) {
            store(keys.authRefreshTokenKey, value)
        }

    /**
     * Stores the value with given key into the storage for later fetching.
     *
     * @param key the key to use for storage.
     * @param value the value to store in storage.
     */
    abstract fun store(key: String, value: String)

    /**
     * Fetches the existing value from the storage and returns it, or null if it doesn't exist.
     *
     * @return the previous stored value for specified key or null if not in storage.
     */
    abstract fun fetch(key: String): String?

    /**
     * Removes specified key from storage.
     */
    abstract fun remove(key: String)

    /**
     * Set of keys Vault will use to access stored data.
     *
     * @param vaultKey the key used to access the Vault itself.
     * @param tokenKey the key used to fetch the token value from the Vault.
     * @param authRefreshTokenKey the key used to fetch the auth refresh token value from the Vault.
     */
    data class Keys(
        val vaultKey: String,
        val tokenKey: String,
        val authRefreshTokenKey: String,
    )
}
