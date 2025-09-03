package com.genesys.cloud.messenger.transport.util

import android.content.Context
import android.content.SharedPreferences
import java.lang.ref.WeakReference

@Deprecated("Use [Vault] instead.")
actual class DefaultTokenStore actual constructor(storeKey: String) : TokenStore() {
    private val sharedPreferences: SharedPreferences

    init {
        if (context == null) {
            throw IllegalStateException("Must set DefaultTokenStore.context before instantiating")
        }
        sharedPreferences = context!!.getSharedPreferences(storeKey, Context.MODE_PRIVATE)
    }

    actual override fun store(token: String) {
        with(sharedPreferences.edit()) {
            putString(TOKEN_KEY, token)
            apply()
        }
    }

    actual override fun fetch(): String? {
        return sharedPreferences.getString(TOKEN_KEY, null)
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
