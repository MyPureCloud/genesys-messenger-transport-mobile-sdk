package com.genesys.cloud.messenger.transport.util

import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogTag
import io.ktor.client.HttpClient
import io.ktor.client.features.HttpCallValidator
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging

internal fun defaultHttpClient(logging: Boolean = false): HttpClient = HttpClient {
    if (logging) {
        install(Logging) {
            this.logger = Log(logging, LogTag.HTTP_CLIENT).ktorLogger
            level = LogLevel.ALL
        }
    }
    install(JsonFeature) {
        serializer = KotlinxSerializer(
            kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                useAlternativeNames = false
            }
        )
    }
    install(HttpCallValidator)
}
