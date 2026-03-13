package com.genesys.cloud.messenger.journey.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class JourneyUrlsTest {

    private val urls = JourneyUrls(domain = "mypurecloud.com", deploymentId = "test-deploy-123")

    @Test
    fun `deploymentConfigUrl is correct`() {
        assertThat(urls.deploymentConfigUrl.toString())
            .isEqualTo("https://api-cdn.mypurecloud.com/webdeployments/v1/deployments/test-deploy-123/config.json")
    }

    @Test
    fun `appEventsUrl is correct`() {
        assertThat(urls.appEventsUrl.toString())
            .isEqualTo("https://api.mypurecloud.com/api/v2/journey/deployments/test-deploy-123/appevents")
    }
}
