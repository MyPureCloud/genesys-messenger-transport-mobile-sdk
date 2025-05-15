package com.genesys.cloud.messenger.transport.util

import android.content.Context
import android.content.SharedPreferences
import com.genesys.cloud.messenger.transport.core.InternalVault
import java.lang.ref.WeakReference

/**
 * Android implementation of EncryptedVault that uses Android KeyStore
 * for the encryption keys and stores the encrypted data in SharedPreferences.
 *
 */
actual class EncryptedVault actual constructor(keys: Keys) :
    Vault(keys) {
    private val sharedPreferences: SharedPreferences
    private val internalVault: InternalVault

    init {
        if (context == null) {
            throw IllegalStateException("Must set EncryptedVault.context before instantiating.")
        }
        sharedPreferences = context!!.getSharedPreferences(keys.vaultKey, Context.MODE_PRIVATE)
        internalVault = InternalVault(keys.vaultKey, sharedPreferences)
    }

    override fun store(key: String, value: String) {
        internalVault.store(key, value)
    }

    override fun fetch(key: String): String? {
        return internalVault.fetch(key)
    }

    override fun remove(key: String) {
        internalVault.remove(key)
    }

    companion object {
        private var contextRef: WeakReference<Context>? = null

        var context: Context?
            get() = contextRef?.get()
            set(value) {
                contextRef = value?.let { WeakReference(it.applicationContext) }
            }
    }
}
