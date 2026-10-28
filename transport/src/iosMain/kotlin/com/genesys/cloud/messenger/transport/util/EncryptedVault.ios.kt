package com.genesys.cloud.messenger.transport.util

import com.genesys.cloud.messenger.transport.core.InternalVault

/**
 * iOS implementation of EncryptedVault that wraps the iOS default vault (Keychain).
 */
actual class EncryptedVault actual constructor(keys: Keys) : Vault(keys) {
    private val vault = InternalVault(keys.vaultKey, keys.tokenKey)

    init {
        migrateFromDefaultVault()
    }

    /**
     * Stores the value with given key into the iOS Keychain.
     *
     * @param key the key to use for storage.
     * @param value the value to store in storage.
     */
    actual override fun store(
        key: String,
        value: String
    ) {
        vault.set(key, value)
    }

    /**
     * Fetches the existing value from the iOS Keychain and returns it, or null if it doesn't exist.
     *
     * @return the previous stored value for specified key or null if not in storage.
     */
    actual override fun fetch(key: String): String? {
        return vault.string(key)
    }

    /**
     * Removes specified key from the iOS Keychain.
     */
    actual override fun remove(key: String) {
        vault.remove(key)
    }

    // The legacy DefaultVault stored items under the VAULT_KEY Keychain service.
    // Keychain items can't be enumerated through InternalVault, so the known keys are migrated one by one.
    private fun migrateFromDefaultVault() {
        if (keys.vaultKey == VAULT_KEY) return
        val legacyVault = InternalVault(VAULT_KEY, keys.tokenKey)
        listOf(keys.tokenKey, keys.authRefreshTokenKey, keys.wasAuthenticated, keys.pushConfigKey)
            .forEach { key ->
                legacyVault.string(key)?.let { legacyValue ->
                    // Keep the legacy entry if the write fails so migration retries on next launch.
                    val migrated = vault.string(key) != null || vault.set(key, legacyValue)
                    if (migrated) legacyVault.remove(key)
                }
            }
    }
}
