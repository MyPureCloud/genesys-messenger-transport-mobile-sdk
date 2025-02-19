package com.genesys.cloud.messenger.transport.push

import kotlinx.serialization.Serializable

@Serializable
internal data class RegisterDeviceTokenRequestBody(
    val deviceToken: String,
    val notificationProvider: PushProvider,
    val language: String,
    val deviceType: String,
)
