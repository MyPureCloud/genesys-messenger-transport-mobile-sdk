package com.genesys.cloud.messenger.journey.storage

import android.content.Context
import android.content.SharedPreferences
import java.lang.ref.WeakReference

private const val PREFS_NAME = "com.genesys.cloud.messenger.journey.cookie"
private const val KEY_COOKIE_ID = "customerCookieId"
private const val KEY_COOKIE_TIMESTAMP = "customerCookieIdTimestamp"
private const val ONE_YEAR_MILLIS = 365L * 24 * 60 * 60 * 1000

internal actual class CookieIdStorage actual constructor() {
    private val sharedPreferences: SharedPreferences
        get() {
            val ctx = context
                ?: throw IllegalStateException("Must set CookieIdStorage.context before using JourneyTracker.")
            return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

    actual fun getCustomerCookieId(): String? {
        val prefs = sharedPreferences
        val cookieId = prefs.getString(KEY_COOKIE_ID, null) ?: return null
        val timestamp = prefs.getLong(KEY_COOKIE_TIMESTAMP, 0L)
        if (System.currentTimeMillis() - timestamp > ONE_YEAR_MILLIS) {
            prefs.edit().remove(KEY_COOKIE_ID).remove(KEY_COOKIE_TIMESTAMP).apply()
            return null
        }
        return cookieId
    }

    actual fun setCustomerCookieId(cookieId: String) {
        sharedPreferences.edit()
            .putString(KEY_COOKIE_ID, cookieId)
            .putLong(KEY_COOKIE_TIMESTAMP, System.currentTimeMillis())
            .apply()
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
