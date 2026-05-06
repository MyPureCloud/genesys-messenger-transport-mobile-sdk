package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.util.CUSTOM_ENDPOINT_ENV_KEY
import com.genesys.cloud.messenger.transport.util.getEnvironmentVariable

/**
 * @param deploymentId the ID of the Genesys Cloud Messenger deployment.
 * @param domain the regional base domain address for a Genesys Cloud Web Messaging service. For example, "mypurecloud.com".
 * @param logging indicates if logging should be enabled.
 * @param reconnectionTimeoutInSeconds period of time during which Transport will try to reconnect to the web socket in case of connectivity lost.
 * @param autoRefreshTokenWhenExpired indicates if Transport should auto refresh auth token if it was expired.
 * @param sessionExpirationNoticeIntervalSeconds how many seconds before the session expires to show the expiration notice
 * @param minimumWebSocketTlsVersion the minimum TLS protocol version for WebSocket connections. Default is [TlsVersion.SYSTEM_DEFAULT] for backward compatibility.
 */
data class Configuration(
    val deploymentId: String,
    val domain: String,
    val logging: Boolean = false,
    val reconnectionTimeoutInSeconds: Long = 60 * 5,
    val autoRefreshTokenWhenExpired: Boolean = true,
    val encryptedVault: Boolean = false,
    val sessionExpirationNoticeIntervalSeconds: Long = DEFAULT_INTERVAL,
    val minimumWebSocketTlsVersion: TlsVersion = TlsVersion.SYSTEM_DEFAULT
) {
    /**
     * Secondary constructor to avoid breaking changes on iOS platform.
     *
     * @param deploymentId the ID of the Genesys Cloud Messenger deployment.
     * @param domain the regional base domain address for a Genesys Cloud Web Messaging service. For example, "mypurecloud.com".
     * @param logging indicates if logging should be enabled.
     * @param reconnectionTimeoutInSeconds period of time during which Transport will try to reconnect to the web socket in case of connectivity lost.
     */
    constructor(
        deploymentId: String,
        domain: String,
        logging: Boolean,
        reconnectionTimeoutInSeconds: Long,
    ) : this(
        deploymentId = deploymentId,
        domain = domain,
        logging = logging,
        reconnectionTimeoutInSeconds = reconnectionTimeoutInSeconds,
        autoRefreshTokenWhenExpired = true,
        encryptedVault = false,
        sessionExpirationNoticeIntervalSeconds = DEFAULT_INTERVAL,
        minimumWebSocketTlsVersion = TlsVersion.SYSTEM_DEFAULT
    )

    /**
     * Secondary constructor to preserve the pre-2.12.0 primary constructor signature for iOS/Swift callers.
     */
    constructor(
        deploymentId: String,
        domain: String,
        logging: Boolean,
        reconnectionTimeoutInSeconds: Long,
        autoRefreshTokenWhenExpired: Boolean,
        encryptedVault: Boolean,
    ) : this(
        deploymentId = deploymentId,
        domain = domain,
        logging = logging,
        reconnectionTimeoutInSeconds = reconnectionTimeoutInSeconds,
        autoRefreshTokenWhenExpired = autoRefreshTokenWhenExpired,
        encryptedVault = encryptedVault,
        sessionExpirationNoticeIntervalSeconds = DEFAULT_INTERVAL,
        minimumWebSocketTlsVersion = TlsVersion.SYSTEM_DEFAULT
    )

    internal var application: String = "TransportSDK-${MessengerTransportSDK.sdkVersion}"

    /**
     * Internal-only endpoint override (host:port) for routing all Transport connections
     * to a local server (e.g., WireMock). Resolved in order:
     * 1. Explicit value set via [InternalConfigurationFactory].
     * 2. `TRANSPORT_CUSTOM_ENDPOINT` environment variable.
     * 3. null → standard domain-derived URLs.
     *
     * Intentionally declared outside the data class constructor so it is excluded from
     * [equals], [hashCode], [copy], and [toString]. Must be manually re-assigned after [copy].
     */
    internal var customEndpoint: String? =
        getEnvironmentVariable(CUSTOM_ENDPOINT_ENV_KEY)?.ifBlank { null }

    companion object {
        internal const val DEFAULT_INTERVAL = 300L
    }
}
