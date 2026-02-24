package com.genesys.cloud.messenger.transport.core

/**
 * Represents the minimum TLS protocol version to enforce for WebSocket connections.
 */
enum class TlsVersion {
    /**
     * Use system default TLS configuration.
     */
    SYSTEM_DEFAULT,

    /**
     * Enforce TLS 1.2 as the minimum version.
     */
    TLS_1_2,

    /**
     * Enforce TLS 1.3 as the minimum version.
     */
    TLS_1_3
}
