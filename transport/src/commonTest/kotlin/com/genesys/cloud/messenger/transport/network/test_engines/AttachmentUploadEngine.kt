package com.genesys.cloud.messenger.transport.network.test_engines

import com.genesys.cloud.messenger.transport.respondNotFound
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

internal const val UPLOAD_FILE_PATH = "http://uploadurl.com"
internal const val UPLOAD_FILE_SIZE = 100L
internal val validHeaders = mapOf("Header" to "Valid")
internal val invalidHeaders = mapOf("Header" to "Invalid")

internal fun HttpClientConfig<MockEngineConfig>.uploadFileEngine() {
    engine {
        addHandler { request ->
            when (request.url.toString()) {
                UPLOAD_FILE_PATH -> {
                    if (request.method == HttpMethod.Put && request.body.contentLength == UPLOAD_FILE_SIZE && request.headers["Header"].equals("Valid")) {
                        // Simulate a successful upload
                        respond(
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            content = ""
                        )
                    } else {
                        respondBadRequest()
                    }
                }
                else -> {
                    respondNotFound()
                }
            }
        }
    }
}
