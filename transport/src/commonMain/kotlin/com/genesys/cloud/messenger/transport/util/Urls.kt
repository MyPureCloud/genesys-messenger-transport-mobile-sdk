package com.genesys.cloud.messenger.transport.util

import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.path

private const val BASE_WEBDEPLOYMENTS_PATH = "api/v2/webdeployments"
private const val BASE_WEBMESSAGING_PATH = "api/v2/webmessaging"

internal class Urls(
    val domain: String,
    val deploymentId: String,
    val application: String,
    customBaseUrl: String? = null
) {
    private val sanitizedBaseUrl: String? = customBaseUrl?.sanitizeBaseUrl()

    private val wsBaseUrl: String by lazy {
        sanitizedBaseUrl?.let { "ws://$it" } ?: "wss://webmessaging.$domain"
    }

    private val apiBaseUrl: Url by lazy {
        URLBuilder(sanitizedBaseUrl?.let { "http://$it" } ?: "https://api.$domain").build()
    }

    private val cdnBaseUrl: String by lazy {
        sanitizedBaseUrl?.let { "http://$it" } ?: "https://api-cdn.$domain"
    }

    internal val webSocketUrl: Url by lazy {
        URLBuilder(wsBaseUrl)
            .apply {
                path("v1")
                parameters.append("deploymentId", deploymentId)
                parameters.append("application", application)
            }.build()
    }

    internal val deploymentConfigUrl: Url by lazy {
        URLBuilder(cdnBaseUrl)
            .apply {
                path("webdeployments/v1/deployments/$deploymentId/config.json")
            }.build()
    }

    internal val history: Url by lazy {
        URLBuilder(apiBaseUrl).apply { path("$BASE_WEBMESSAGING_PATH/messages") }.build()
    }

    internal val jwtAuthUrl: Url by lazy {
        URLBuilder(apiBaseUrl).apply { path("$BASE_WEBDEPLOYMENTS_PATH/token/oauthcodegrantjwtexchange") }.build()
    }

    internal val logoutUrl: Url by lazy {
        URLBuilder(apiBaseUrl).apply { path("$BASE_WEBDEPLOYMENTS_PATH/token/revoke") }.build()
    }

    internal val refreshAuthTokenUrl: Url by lazy {
        URLBuilder(apiBaseUrl).apply { path("$BASE_WEBDEPLOYMENTS_PATH/token/refresh") }.build()
    }

    internal val deviceTokenUrl: (String, String) -> Url = { deploymentId, token ->
        URLBuilder(apiBaseUrl)
            .apply { path("$BASE_WEBMESSAGING_PATH/deployments/$deploymentId/pushdevices/$token") }
            .build()
    }
}

// Matches URL scheme prefixes like "http://", "https://", "ws://", etc.
private val schemeRegex = Regex("^\\w+://")

/**
 * Normalizes a raw custom base URL input into a clean "host:port" form.
 *
 * - Strips any scheme prefix (e.g., "http://localhost:8080" → "localhost:8080")
 * - Removes trailing slashes (e.g., "localhost:8080/" → "localhost:8080")
 * - Returns null if the result is blank, causing fallback to production URLs.
 */
private fun String.sanitizeBaseUrl(): String? {
    val stripped = schemeRegex.replace(this, "").trimEnd('/')
    return stripped.ifBlank { null }
}
