package com.genesys.cloud.messenger.transport

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.MessengerTransportSDK
import com.genesys.cloud.messenger.transport.util.Urls
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.ktor.http.Url
import kotlin.test.Test

class UrlsTest {
    private val subject = Urls(TestValues.DOMAIN, TestValues.DEPLOYMENT_ID, TestValues.application)

    @Test
    fun `it should get webSocketUrl`() {
        val expected = Url("wss://webmessaging.${TestValues.DOMAIN}/v1?deploymentId=${TestValues.DEPLOYMENT_ID}&application=TransportSDK-${MessengerTransportSDK.sdkVersion}")

        val result = subject.webSocketUrl

        assertThat(result, "WebSocket URL").isEqualTo(expected)
    }

    @Test
    fun `it should get deploymentConfigUrl`() {
        val expected = Url("https://api-cdn.${TestValues.DOMAIN}/webdeployments/v1/deployments/${TestValues.DEPLOYMENT_ID}/config.json")

        val result = subject.deploymentConfigUrl

        assertThat(result, "Deployment config URL").isEqualTo(expected)
    }

    @Test
    fun `it should get history`() {
        val expected = Url("https://api.${TestValues.DOMAIN}/api/v2/webmessaging/messages")

        val result = subject.history

        assertThat(result, "history URL").isEqualTo(expected)
    }

    @Test
    fun `it should get jwtAuthUrl`() {
        val expected = Url("https://api.${TestValues.DOMAIN}/api/v2/webdeployments/token/oauthcodegrantjwtexchange")

        val result = subject.jwtAuthUrl

        assertThat(result, "jwtAuth config URL").isEqualTo(expected)
    }

    @Test
    fun `it should get logoutUrl`() {
        val expected = Url("https://api.${TestValues.DOMAIN}/api/v2/webdeployments/token/revoke")

        val result = subject.logoutUrl

        assertThat(result, "logoutUrl URL").isEqualTo(expected)
    }

    @Test
    fun `it should get refreshAuthTokenUrl`() {
        val expected = Url("https://api.${TestValues.DOMAIN}/api/v2/webdeployments/token/refresh")

        val result = subject.refreshAuthTokenUrl

        assertThat(result, "refreshAuthToken URL").isEqualTo(expected)
    }

    @Test
    fun `it should get deviceTokenUrl`() {
        val expected =
            Url("https://api.${TestValues.DOMAIN}/api/v2/webmessaging/deployments/${TestValues.DEPLOYMENT_ID}/pushdevices/${TestValues.TOKEN}")

        val result = subject.deviceTokenUrl(TestValues.DEPLOYMENT_ID, TestValues.TOKEN)

        assertThat(result, "registerDeviceToken URL").isEqualTo(expected)
    }
}
