package com.genesys.cloud.messenger.journey.config

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.journey.network.JourneyUrls
import com.genesys.cloud.messenger.journey.util.logs.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DeploymentConfigCheckerTest {

    private val urls = JourneyUrls("mypurecloud.com", "deploy-123")
    private val log = Log(enableLogs = false, tag = "Test")

    private fun createChecker(responseBody: String, statusCode: HttpStatusCode = HttpStatusCode.OK): DeploymentConfigChecker {
        val mockEngine = MockEngine { _ ->
            respond(
                content = responseBody,
                status = statusCode,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
        return DeploymentConfigChecker(urls, client, log)
    }

    @Test
    fun `returns true when journeyEvents is enabled`() = runTest {
        val checker = createChecker("""{"journeyEvents":{"enabled":true},"id":"x","version":"1"}""")
        val result = checker.check()
        assertThat(result).isTrue()
        assertThat(checker.isTrackingEnabled!!).isTrue()
    }

    @Test
    fun `returns false when journeyEvents is disabled`() = runTest {
        val checker = createChecker("""{"journeyEvents":{"enabled":false},"id":"x","version":"1"}""")
        val result = checker.check()
        assertThat(result).isFalse()
        assertThat(checker.isTrackingEnabled!!).isFalse()
    }

    @Test
    fun `returns false on API error`() = runTest {
        val checker = createChecker("Server Error", HttpStatusCode.InternalServerError)
        val result = checker.check()
        assertThat(result).isFalse()
    }

    @Test
    fun `returns false on invalid JSON`() = runTest {
        val checker = createChecker("not json at all")
        val result = checker.check()
        assertThat(result).isFalse()
    }
}
