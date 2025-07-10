package com.genesys.cloud.messenger.transport.push

import com.genesys.cloud.messenger.transport.util.UNKNOWN
import com.genesys.cloud.messenger.transport.util.UNKNOWN_LONG
import kotlinx.serialization.Serializable

internal const val DEVICE_TOKEN_EXPIRATION_IN_MILLISECONDS = 2_592_000_000L // 30 days in milliseconds

@Serializable
internal data class PushConfig(
    val token: String,
    val deviceToken: String,
    val preferredLanguage: String,
    val lastSyncTimestamp: Long,
    val deviceType: String,
    val pushProvider: PushProvider?,
)

internal val DEFAULT_PUSH_CONFIG = PushConfig(
    token = UNKNOWN,
    deviceToken = UNKNOWN,
    preferredLanguage = UNKNOWN,
    lastSyncTimestamp = UNKNOWN_LONG,
    deviceType = UNKNOWN,
    pushProvider = null,
)
