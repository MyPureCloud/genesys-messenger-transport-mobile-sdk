package com.genesys.cloud.messenger.transport.core

/**
 * Factory for creating [Configuration] instances.
 * This object is intended for internal usage only.
 */
object InternalConfigurationFactory {
    /**
     * @param deploymentId the ID of the Genesys Cloud Messenger deployment.
     * @param domain the regional base domain address for a Genesys Cloud Web Messaging service.
     * @param applicationType the type of application using the configuration.
     * @param applicationVersion the version of application using the configuration.
     * @param logging indicates if logging should be enabled.
     * @param reconnectionTimeoutInSeconds period of time during which Transport will try to reconnect.
     * @param autoRefreshTokenWhenExpired indicates if Transport should auto refresh auth token if expired.
     * @param encryptedVault indicates if encrypted vault should be used.
     * @param minimumTlsVersion the minimum TLS protocol version for secure connections (iOS only).
     * @return Configuration instance with proper application parameter formatting.
     */
    fun create(
        deploymentId: String,
        domain: String,
        applicationType: ApplicationType,
        applicationVersion: String,
        logging: Boolean = false,
        reconnectionTimeoutInSeconds: Long = 60 * 5,
        autoRefreshTokenWhenExpired: Boolean = true,
        encryptedVault: Boolean = false,
        minimumTlsVersion: TlsVersion = TlsVersion.SYSTEM_DEFAULT
    ): Configuration {
        val config =
            Configuration(
                deploymentId = deploymentId,
                domain = domain,
                logging = logging,
                reconnectionTimeoutInSeconds = reconnectionTimeoutInSeconds,
                autoRefreshTokenWhenExpired = autoRefreshTokenWhenExpired,
                encryptedVault = encryptedVault,
                minimumTlsVersion = minimumTlsVersion
            )

        config.application =
            when (applicationType) {
                ApplicationType.TRANSPORT_SDK -> "TransportSDK-${MessengerTransportSDK.sdkVersion}"
                else -> "${applicationType.title}-$applicationVersion/TransportSDK-${MessengerTransportSDK.sdkVersion}"
            }
        return config
    }

    /**
     * Overload to preserve the pre-2.12.0 signature for iOS/Swift callers.
     */
    fun create(
        deploymentId: String,
        domain: String,
        applicationType: ApplicationType,
        applicationVersion: String,
        logging: Boolean,
        reconnectionTimeoutInSeconds: Long,
        autoRefreshTokenWhenExpired: Boolean,
        encryptedVault: Boolean
    ): Configuration = create(
        deploymentId = deploymentId,
        domain = domain,
        applicationType = applicationType,
        applicationVersion = applicationVersion,
        logging = logging,
        reconnectionTimeoutInSeconds = reconnectionTimeoutInSeconds,
        autoRefreshTokenWhenExpired = autoRefreshTokenWhenExpired,
        encryptedVault = encryptedVault,
        minimumTlsVersion = TlsVersion.SYSTEM_DEFAULT
    )
}
