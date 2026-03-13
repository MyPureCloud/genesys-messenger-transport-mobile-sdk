package com.genesys.cloud.messenger.journey.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AppEventRequest(
    val eventName: String,
    val screenName: String,
    val app: App,
    val device: Device,
    val sdkLibrary: SdkLibrary,
    val networkConnectivity: NetworkConnectivity? = null,
    val referrerUrl: String? = null,
    val searchQuery: String? = null,
    val attributes: Map<String, JsonElement>? = null,
    val traits: Map<String, String>? = null,
    val externalId: String? = null,
    val customerCookieId: String,
    val createdDate: String,
)

@Serializable
data class App(
    val name: String,
    val namespace: String,
    val version: String,
    val buildNumber: String,
)

@Serializable
data class Device(
    val category: String,
    val type: String,
    val osFamily: String,
    val osVersion: String,
    val isMobile: Boolean? = null,
    val screenHeight: Int? = null,
    val screenWidth: Int? = null,
    val screenDensity: Int? = null,
    val fingerprint: String? = null,
    val manufacturer: String? = null,
)

@Serializable
data class SdkLibrary(
    val name: String,
    val version: String,
)

@Serializable
data class NetworkConnectivity(
    val carrier: String? = null,
    val bluetoothEnabled: Boolean? = null,
    val cellularEnabled: Boolean? = null,
    val wifiEnabled: Boolean? = null,
)
