package com.genesys.cloud.messenger.transport.util

import android.content.Context
import android.content.SharedPreferences
import java.lang.ref.WeakReference

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

    override fun storeAuthRefreshToken(refreshToken: String) {
        with(sharedPreferences.edit()) {
            putString(AUTH_REFRESH_TOKEN_KEY, refreshToken)
            apply()
        }
    }

    override fun fetchAuthRefreshToken(): String? {
        return sharedPreferences.getString(AUTH_REFRESH_TOKEN_KEY, null)
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
