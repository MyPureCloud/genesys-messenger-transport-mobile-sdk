package com.genesys.cloud.messenger.transport.network

import okhttp3.Protocol

/**
 * Shared HTTP/2 protocol configuration for the Android platform.
 * Both the REST client (Ktor/OkHttp engine) and the WebSocket client use these protocol lists
 * so that HTTP/2 is preferred when the server supports it via ALPN.
 */

/** Protocol list for the platform HTTP engine (REST client). HTTP/2 preferred, then HTTP/1.1. */
internal val PLATFORM_HTTP_ENGINE_PROTOCOLS = listOf(Protocol.HTTP_2, Protocol.HTTP_1_1)

/** Protocol list for the WebSocket OkHttp client. HTTP/2 preferred, then HTTP/1.1. */
internal val WEB_SOCKET_OKHTTP_PROTOCOLS = listOf(Protocol.HTTP_2, Protocol.HTTP_1_1)
