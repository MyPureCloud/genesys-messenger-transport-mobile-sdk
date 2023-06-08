package com.genesys.cloud.messenger.transport.util

internal const val VAULT_KEY = "com.genesys.cloud.messenger"
internal const val TOKEN_KEY = "token"
internal const val AUTH_REFRESH_TOKEN_KEY = "auth_refresh_token"

/**
 * A default implementation of the [Vault] that should be sufficient for most users of the
 * SDK.
 */
expect class DefaultVault(
    keys: Keys = Keys(
        vaultKey = VAULT_KEY,
        tokenKey = TOKEN_KEY,
        authRefreshTokenKey = AUTH_REFRESH_TOKEN_KEY,
    ),
) : Vault
