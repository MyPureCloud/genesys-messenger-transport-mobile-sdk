package com.genesys.cloud.messenger.transport.util

/**
 * A store for the session token. Users of the library can use the [DefaultTokenStore]
 * implementation for simplicity or create a custom token store by making a concrete implementation
 * of this class and passing it in to the [MessengerTransport] constructor.
 */
@Deprecated("Use [Vault] instead.")
abstract class TokenStore {
    internal val token: String
        get() = fetch() ?: TokenGenerator.generate().also {
            store(it)
        }

    /**
     * Stores the token into the storage for later fetching.
     *
     * @param token the token to store in storage
     */
    abstract fun store(token: String)

    /**
     * Fetches the existing token from the token store and returns it, or null if it doesn't exist.
     *
     * @return the previous stored token or null if no token in storage
     */
    abstract fun fetch(): String?

    /**
     * Stores the auth refresh token into the storage for later fetching.
     *
     * @param refreshToken the auth refresh token to store in storage.
     */
    abstract fun storeAuthRefreshToken(refreshToken: String)

    /**
     * Fetches the existing auth refresh token from the token store and returns it, or null if it doesn't exist.
     *
     * @return the previous stored auth refresh token or null if no token in storage
     */
    abstract fun fetchAuthRefreshToken(): String?
}
