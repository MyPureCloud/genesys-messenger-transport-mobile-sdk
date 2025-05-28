package com.genesys.cloud.messenger.transport.util

import android.content.Context
import android.content.SharedPreferences
import java.lang.ref.WeakReference

actual class DefaultVault actual constructor(keys: Keys) : Vault(keys) {
    private val sharedPreferences: SharedPreferences

    init {
        val currentContext = context ?: throw IllegalStateException("Must set DefaultVault.context before instantiating.")
        sharedPreferences = currentContext.getSharedPreferences(keys.vaultKey, Context.MODE_PRIVATE)
    }

    actual override fun store(key: String, value: String) {
        with(sharedPreferences.edit()) {
            putString(key, value)
            apply()
        }
    }

    actual override fun fetch(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    actual override fun remove(key: String) {
        with(sharedPreferences.edit()) {
            remove(key)
            apply()
        }
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
