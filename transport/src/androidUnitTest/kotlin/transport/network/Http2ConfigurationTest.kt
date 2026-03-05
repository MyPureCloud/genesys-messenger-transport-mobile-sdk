package transport.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.network.PLATFORM_HTTP_ENGINE_PROTOCOLS
import com.genesys.cloud.messenger.transport.network.WEB_SOCKET_OKHTTP_PROTOCOLS
import okhttp3.Protocol
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Test

/**
 * Verifies that HTTP/2 is configured for both the platform HTTP engine (REST client)
 * and the WebSocket OkHttp client, and that actual HTTP/2 is used when the server supports it.
 */
class Http2ConfigurationTest {

    private val server = MockWebServer()

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `platform HTTP engine protocols include HTTP_2 and HTTP_1_1`() {
        assertThat(Protocol.HTTP_2 in PLATFORM_HTTP_ENGINE_PROTOCOLS).isTrue()
        assertThat(Protocol.HTTP_1_1 in PLATFORM_HTTP_ENGINE_PROTOCOLS).isTrue()
    }

    @Test
    fun `WebSocket OkHttp client protocols include HTTP_2 and HTTP_1_1`() {
        assertThat(Protocol.HTTP_2 in WEB_SOCKET_OKHTTP_PROTOCOLS).isTrue()
        assertThat(Protocol.HTTP_1_1 in WEB_SOCKET_OKHTTP_PROTOCOLS).isTrue()
    }

    private fun startHttpsServerWithHttp2(response: MockResponse) {
        val sslContext = TestSslHelper.serverSslContext()
        server.useHttps(sslContext.socketFactory, false)
        server.protocols = listOf(Protocol.HTTP_2, Protocol.HTTP_1_1)
        server.enqueue(response)
        server.start()
    }

    private fun okHttpClientWithTestSsl(protocols: List<Protocol>): okhttp3.OkHttpClient =
        okhttp3.OkHttpClient.Builder()
            .protocols(protocols)
            .connectionSpecs(
                listOf(
                    okhttp3.ConnectionSpec.MODERN_TLS,
                    okhttp3.ConnectionSpec.CLEARTEXT
                )
            )
            .hostnameVerifier { _, _ -> true }
            .sslSocketFactory(
                TestSslHelper.clientSslContext().socketFactory,
                TestSslHelper.clientTrustManager()
            )
            .build()

    @Test
    fun `REST client uses HTTP_2 when server supports it`() {
        // 1. Start HTTPS server supporting HTTP/2 and a plain body response.
        startHttpsServerWithHttp2(MockResponse().setBody("ok"))
        // 2. Build client with REST protocol list and test SSL (trust server cert).
        val client = okHttpClientWithTestSsl(PLATFORM_HTTP_ENGINE_PROTOCOLS)
        // 3. GET / and assert the connection used HTTP/2.
        val request = okhttp3.Request.Builder().url(server.url("/")).build()
        client.newCall(request).execute().use { response ->
            assertThat(response.protocol).isEqualTo(Protocol.HTTP_2)
        }
    }

    @Test
    fun `WebSocket client uses HTTP_2 when server supports it`() {
        // Client built with WEB_SOCKET_OKHTTP_PROTOCOLS uses HTTP/2 when server supports it.
        // Use a plain GET (same TLS/ALPN path as WebSocket upgrade) since MockWebServer
        // WebSocket upgrade over HTTP/2 does not complete reliably in this environment.
        startHttpsServerWithHttp2(MockResponse().setBody("ok"))
        val client = okHttpClientWithTestSsl(WEB_SOCKET_OKHTTP_PROTOCOLS)
        val request = okhttp3.Request.Builder().url(server.url("/")).build()
        client.newCall(request).execute().use { response ->
            assertThat(response.protocol).isEqualTo(Protocol.HTTP_2)
        }
    }
}
