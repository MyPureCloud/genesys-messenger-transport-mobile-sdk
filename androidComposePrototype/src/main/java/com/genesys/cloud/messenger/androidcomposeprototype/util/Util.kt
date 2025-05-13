package com.genesys.cloud.messenger.androidcomposeprototype.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log

private const val SHARED_PREFERENCES_NAME = "com.genesys.cloud.messenger.androidcomposeprototype"

fun Context.getSharedPreferences(): SharedPreferences {
    return getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
}

fun Uri.getParam(param: String): String? {
    return fragment?.let { currentFragment ->
        Log.d("UriParsing", "Fragment: $currentFragment")
        // The fragment is a string like "id_token=eyJraWQiOiJyS0px&access_token=..."

        val params = mutableMapOf<String, String>()
        val pairs = currentFragment.split("&")
        for (pair in pairs) {
            val parts = pair.split("=")
            if (parts.size == 2) {
                val key = Uri.decode(parts[0]) // Decode URL-encoded parts
                val value = Uri.decode(parts[1])
                params[key] = value
            }
        }

        val paramValue = params[param]

        if (paramValue != null) {
            Log.d("UriParsing", "Extracted $param: $paramValue")
            paramValue
        } else {
            Log.e("UriParsing", "$param not found in fragment")
            null
        }
    } ?: run {
        Log.e("UriParsing", "no fragment found")
        null
    }
}
