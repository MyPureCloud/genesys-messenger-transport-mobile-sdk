package com.genesys.cloud.messenger.androidcomposeprototype.util

import android.content.Context
import android.content.SharedPreferences
import com.genesys.cloud.messenger.androidcomposeprototype.BuildConfig

const val OKTA_AUTHORIZE_URL = """https://${BuildConfig.OKTA_DOMAIN}/oauth2/default/v1/authorize?client_id=${BuildConfig.CLIENT_ID}&response_type=code&scope=openid profile&redirect_uri=${BuildConfig.SIGN_IN_REDIRECT_URI}&state=${BuildConfig.OKTA_STATE}"""
private const val SHARED_PREFERENCES_NAME = "com.genesys.cloud.messenger.androidcomposeprototype"

fun Context.getSharedPreferences(): SharedPreferences {
    return getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
}
