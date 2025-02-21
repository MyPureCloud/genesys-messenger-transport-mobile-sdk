package com.genesys.cloud.messenger.transport.push

import kotlinx.serialization.Serializable

@Serializable
internal data class DeviceTokenRequestBody(
    val deviceToken: String,
    val language: String,
    val deviceType: String? = null,
    val notificationProvider: PushProvider? = null,
)
