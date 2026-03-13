package com.genesys.cloud.messenger.journey

import android.content.Context
import com.genesys.cloud.messenger.journey.storage.CookieIdStorage

internal actual fun initPlatformStorage(context: Any?) {
    (context as? Context)?.let { CookieIdStorage.context = it }
}
