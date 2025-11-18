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

    actual override fun store(
        key: String,
        value: String
    ) {
        synchronized(sharedPreferences) {
            sharedPreferences.edit().putString(key, value).commit()
        }
    }

    actual override fun fetch(key: String): String? {
        synchronized(sharedPreferences) {
            return sharedPreferences.getString(key, null)
        }
    }

    actual override fun remove(key: String) {
        synchronized(sharedPreferences) {
            sharedPreferences.edit().remove(key).commit()
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
