package com.genesys.cloud.messenger.transport.core

/**
 * @param deploymentId the ID of the Genesys Cloud Messenger deployment.
 * @param domain the regional base domain address for a Genesys Cloud Web Messaging service. For example, "mypurecloud.com".
 * @param logging indicates if logging should be enabled.
 * @param reconnectionTimeoutInSeconds period of time during which Transport will try to reconnect to the web socket in case of connectivity lost.
 * @param autoRefreshTokenWhenExpired indicates if Transport should auto refresh auth token if it was expired.
 * @param minimumTlsVersion the minimum TLS protocol version for WebSocket connections (iOS only). Does not affect HTTP REST calls. Default is [TlsVersion.SYSTEM_DEFAULT] for backward compatibility.
 */
data class Configuration(
    val deploymentId: String,
    val domain: String,
    val logging: Boolean = false,
    val reconnectionTimeoutInSeconds: Long = 60 * 5,
    val autoRefreshTokenWhenExpired: Boolean = true,
    val encryptedVault: Boolean = false,
    val minimumTlsVersion: TlsVersion = TlsVersion.SYSTEM_DEFAULT
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
        minimumTlsVersion = TlsVersion.SYSTEM_DEFAULT
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
        minimumTlsVersion = TlsVersion.SYSTEM_DEFAULT
    )

    internal var application: String = "TransportSDK-${MessengerTransportSDK.sdkVersion}"
}
