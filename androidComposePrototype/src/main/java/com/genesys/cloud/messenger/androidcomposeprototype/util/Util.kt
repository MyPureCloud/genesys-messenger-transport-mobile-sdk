package com.genesys.cloud.messenger.androidcomposeprototype.util

import android.content.Context
import android.content.SharedPreferences

private const val SHARED_PREFERENCES_NAME = "com.genesys.cloud.messenger.androidcomposeprototype"

fun Context.getSharedPreferences(): SharedPreferences {
    return getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
}
