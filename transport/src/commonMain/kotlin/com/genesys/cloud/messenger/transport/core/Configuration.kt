package com.genesys.cloud.messenger.transport.core

/**
 * @param deploymentId the ID of the Genesys Cloud Messenger deployment.
 * @param domain the regional base domain address for a Genesys Cloud Web Messaging service. For example, "mypurecloud.com".
 * @param logging indicates if logging should be enabled.
 * @param reconnectionTimeoutInSeconds period of time during which Transport will try to reconnect to the web socket in case of connectivity lost.
 * @param autoRefreshTokenWhenExpired indicates if Transport should auto refresh auth token if it was expired.
 * @param maxRetryAfterWaitSeconds maximum time in seconds to wait for 429 Retry-After header. If Retry-After exceeds this value, the request will fail immediately.
 * @param retryEnabled indicates if automatic retries should be enabled for REST API calls.
 * @param backoffEnabled indicates if exponential backoff should be enabled for 5xx server errors.
 */
data class Configuration(
    val deploymentId: String,
    val domain: String,
    val logging: Boolean = false,
    val reconnectionTimeoutInSeconds: Long = 60 * 5,
    val autoRefreshTokenWhenExpired: Boolean = true,
    val encryptedVault: Boolean = false,
    val maxRetryAfterWaitSeconds: Int = 30,
    val retryEnabled: Boolean = true,
    val backoffEnabled: Boolean = true
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
        maxRetryAfterWaitSeconds = 30,
        retryEnabled = true,
        backoffEnabled = true
    )

    internal var application: String = "TransportSDK-${MessengerTransportSDK.sdkVersion}"
}
