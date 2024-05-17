package com.genesys.cloud.messenger.transport.util

import com.genesys.cloud.messenger.transport.core.InternalVault

actual class DefaultVault actual constructor(keys: Keys) : Vault(keys) {

    private val vault = InternalVault(keys.vaultKey)

    override fun store(key: String, value: String) {
        vault.set(key, value)
    }

    override fun fetch(key: String): String? {
        return vault.string(key)
    }

    override fun remove(key: String) {
        vault.remove(key)
    }
}
