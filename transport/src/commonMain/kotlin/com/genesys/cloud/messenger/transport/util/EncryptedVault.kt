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
    ),
) : Vault
