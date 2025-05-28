package com.genesys.cloud.messenger.transport.util

/**
 * A encrypted implementation of the [Vault].
 */
expect class EncryptedVault(
    keys: Keys = Keys(
        vaultKey = VAULT_KEY,
        tokenKey = TOKEN_KEY,
        authRefreshTokenKey = AUTH_REFRESH_TOKEN_KEY,
        wasAuthenticated = WAS_AUTHENTICATED,
    ),
) : Vault {
    override fun store(key: String, value: String)

    override fun fetch(key: String): String?

    override fun remove(key: String)
}