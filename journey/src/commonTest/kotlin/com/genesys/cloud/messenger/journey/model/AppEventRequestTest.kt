package com.genesys.cloud.messenger.journey.model

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class AppEventRequestTest {

    private val json = Json {
        encodeDefaults = false
    }

    private fun createRequest(
        networkConnectivity: NetworkConnectivity? = null,
        searchQuery: String? = null,
        traits: Map<String, String>? = null,
        externalId: String? = null,
        referrerUrl: String? = null,
    ) = AppEventRequest(
        eventName = "screen_viewed",
        screenName = "HomeScreen",
        app = App(name = "TestApp", namespace = "com.test", version = "1.0", buildNumber = "1"),
        device = Device(category = "mobile", type = "iPhone", osFamily = "iOS", osVersion = "17.0"),
        sdkLibrary = SdkLibrary(name = "TestSDK", version = "1.0.0"),
        networkConnectivity = networkConnectivity,
        searchQuery = searchQuery,
        traits = traits,
        externalId = externalId,
        referrerUrl = referrerUrl,
        customerCookieId = "test-cookie-id",
        createdDate = "2026-01-01T00:00:00",
    )

    @Test
    fun `serializes required fields correctly`() {
        val request = createRequest()
        val serialized = json.encodeToString(request)
        assertThat(serialized).contains("\"eventName\":\"screen_viewed\"")
        assertThat(serialized).contains("\"screenName\":\"HomeScreen\"")
        assertThat(serialized).contains("\"customerCookieId\":\"test-cookie-id\"")
    }

    @Test
    fun `omits null optional fields`() {
        val request = createRequest()
        val serialized = json.encodeToString(request)
        assertThat(serialized).doesNotContain("networkConnectivity")
        assertThat(serialized).doesNotContain("searchQuery")
        assertThat(serialized).doesNotContain("traits")
        assertThat(serialized).doesNotContain("externalId")
        assertThat(serialized).doesNotContain("referrerUrl")
    }

    @Test
    fun `includes optional fields when set`() {
        val request = createRequest(
            networkConnectivity = NetworkConnectivity(carrier = "AT&T", wifiEnabled = true),
            searchQuery = "shoes",
            traits = mapOf("email" to "test@example.com"),
            externalId = "ext-123",
            referrerUrl = "https://example.com",
        )
        val serialized = json.encodeToString(request)
        assertThat(serialized).contains("\"carrier\":\"AT&T\"")
        assertThat(serialized).contains("\"wifiEnabled\":true")
        assertThat(serialized).contains("\"searchQuery\":\"shoes\"")
        assertThat(serialized).contains("\"email\":\"test@example.com\"")
        assertThat(serialized).contains("\"externalId\":\"ext-123\"")
        assertThat(serialized).contains("\"referrerUrl\":\"https://example.com\"")
    }

    @Test
    fun `device optional fields omitted when null`() {
        val device = Device(category = "mobile", type = "iPhone", osFamily = "iOS", osVersion = "17.0")
        val serialized = json.encodeToString(device)
        assertThat(serialized).doesNotContain("isMobile")
        assertThat(serialized).doesNotContain("screenHeight")
        assertThat(serialized).doesNotContain("manufacturer")
    }

    @Test
    fun `device optional fields included when set`() {
        val device = Device(
            category = "mobile",
            type = "iPhone",
            osFamily = "iOS",
            osVersion = "17.0",
            isMobile = true,
            screenHeight = 844,
            manufacturer = "Apple",
        )
        val serialized = json.encodeToString(device)
        assertThat(serialized).contains("\"isMobile\":true")
        assertThat(serialized).contains("\"screenHeight\":844")
        assertThat(serialized).contains("\"manufacturer\":\"Apple\"")
    }

    @Test
    fun `app serializes all fields`() {
        val app = App(name = "MyApp", namespace = "com.my.app", version = "2.0", buildNumber = "42")
        val serialized = json.encodeToString(app)
        val deserialized = json.decodeFromString<App>(serialized)
        assertThat(deserialized).isEqualTo(app)
    }

    @Test
    fun `sdkLibrary serializes correctly`() {
        val sdk = SdkLibrary(name = "GenesysCloudMessengerJourney", version = "2.11.0")
        val serialized = json.encodeToString(sdk)
        assertThat(serialized).contains("\"name\":\"GenesysCloudMessengerJourney\"")
        assertThat(serialized).contains("\"version\":\"2.11.0\"")
    }
}
