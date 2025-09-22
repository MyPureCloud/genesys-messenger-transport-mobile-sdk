package com.genesys.cloud.messenger.transport.util

internal const val ENCRYPTED_VAULT_KEY = "com.genesys.cloud.messenger.encrypted"

/**
 * A encrypted implementation of the [Vault].
 */
expect class EncryptedVault(
    keys: Keys = Keys(
        vaultKey = ENCRYPTED_VAULT_KEY,
        tokenKey = TOKEN_KEY,
        authRefreshTokenKey = AUTH_REFRESH_TOKEN_KEY,
        wasAuthenticated = WAS_AUTHENTICATED,
        pushConfigKey = PUSH_CONFIG_KEY,
    ),
) : Vault {
    override fun store(key: String, value: String)

    override fun fetch(key: String): String?

    override fun remove(key: String)
}
