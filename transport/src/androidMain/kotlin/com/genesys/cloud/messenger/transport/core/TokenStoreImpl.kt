package com.genesys.cloud.messenger.transport.core

import android.content.Context
import android.content.SharedPreferences
import com.genesys.cloud.messenger.transport.util.TOKEN_KEY
import com.genesys.cloud.messenger.transport.util.TokenStore
import java.util.UUID

internal class TokenStoreImpl(context: Context, storeKey: String) : TokenStore {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        storeKey,
        Context.MODE_PRIVATE
    )

    override val token: String
        get() = sharedPreferences.getString(TOKEN_KEY, null) ?: UUID.randomUUID().toString().also {
            store(it)
        }

    private fun store(value: String) {
        with(sharedPreferences.edit()) {
            putString(TOKEN_KEY, value)
            apply()
        }
    }
}
