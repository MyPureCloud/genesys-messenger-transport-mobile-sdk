package com.genesys.cloud.messenger.transport.push

internal data class PushConfig(
    val token: String,
    val deviceToken: String,
    val preferredLanguage: String,
    val lastSyncTimestamp: Long,
    val deviceType: String,
    val pushProvider: PushProvider,
)
