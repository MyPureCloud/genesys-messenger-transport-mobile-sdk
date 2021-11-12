package com.genesys.cloud.messenger.transport.util

import java.util.UUID

actual class Platform {
    actual val platform: String = "Android ${android.os.Build.VERSION.SDK_INT}"

    actual fun randomUUID() = UUID.randomUUID().toString()

    actual fun epochMillis(): Long = System.currentTimeMillis()
}
