package com.genesys.cloud.messenger.transport.network

import io.ktor.client.HttpClient

/**
 * Creates the default HTTP client using the platform engine (OkHttp/Darwin).
 * Ktor owns the engine lifecycle, so [HttpClient.close()] shuts down the connection pool.
 */
internal expect fun createPlatformHttpClient(logging: Boolean): HttpClient
