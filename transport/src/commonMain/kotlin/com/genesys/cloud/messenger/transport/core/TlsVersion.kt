package com.genesys.cloud.messenger.transport.core

/**
 * Represents the minimum TLS protocol version to enforce for WebSocket connections.
 *
 * This configuration applies only to the iOS WebSocket connection (NSURLSession).
 * It does not affect HTTP REST calls (e.g., deployment config, attachments) which
 * use a separate HTTP client with system-default TLS settings.
 * On Android, the platform handles TLS version negotiation automatically.
 *
 * Note: Genesys Cloud backend has not supported TLS versions below 1.2 since July 2020.
 * See: https://help.genesys.cloud/announcements/deprecation-tls-1-1-protocol/
 *
 * @since 2.12.0
 */
enum class TlsVersion {
    /**
     * Use system default TLS configuration.
     *
     * Maintains the current SDK behavior where the iOS system determines
     * which TLS versions to advertise. This is the default value to ensure
     * zero breaking changes.
     */
    SYSTEM_DEFAULT,

    /**
     * Enforce TLS 1.2 as the minimum version.
     *
     * Widely supported and meets most compliance requirements (PCI DSS 3.2.1+).
     * This is the minimum version supported by Genesys Cloud backend.
     */
    TLS_1_2,

    /**
     * Enforce TLS 1.3 as the minimum version.
     *
     * Use for highest security and strict compliance requirements
     * (e.g., PCI DSS 4.0+, banking regulations, healthcare).
     */
    TLS_1_3
}
