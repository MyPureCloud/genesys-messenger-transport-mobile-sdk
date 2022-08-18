package com.genesys.cloud.messenger.transport.util

import android.content.Context
import android.content.SharedPreferences

actual class DefaultTokenStore actual constructor(storeKey: String) : TokenStore() {
    private val sharedPreferences: SharedPreferences
    init {
        if (context == null) {
            throw IllegalStateException("Must set DefaultTokenStore.context before instantiating")
        }
        sharedPreferences = context!!.getSharedPreferences(storeKey, Context.MODE_PRIVATE)
    }

    override fun store(token: String) {
        with(sharedPreferences.edit()) {
            putString(TOKEN_KEY, token)
            apply()
        }
    }

    override fun fetch(): String? {
        return sharedPreferences.getString(TOKEN_KEY, null)
    }

    companion object {
        var context: Context? = null
            set(value) {
                field = value?.applicationContext
            }
    }
}
