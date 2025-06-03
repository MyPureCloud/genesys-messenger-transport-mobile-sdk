package com.genesys.cloud.messenger.transport.core

object ConfigurationFactory {
    /**
     * @param deploymentId the ID of the Genesys Cloud Messenger deployment.
     * @param domain the regional base domain address for a Genesys Cloud Web Messaging service.
     * @param applicationType the type of SDK being used (Transport or Messenger). Defaults to TRANSPORT.
     * @param messengerSDKVersion the version of the Messenger SDK (required only when sdkType is MESSENGER).
     * @param logging indicates if logging should be enabled.
     * @param reconnectionTimeoutInSeconds period of time during which Transport will try to reconnect.
     * @param autoRefreshTokenWhenExpired indicates if Transport should auto refresh auth token if expired.
     * @param encryptedVault indicates if encrypted vault should be used.
     * @return Configuration instance with proper application parameter formatting.
     */
    fun create(
        deploymentId: String,
        domain: String,
        applicationType: ApplicationType = ApplicationType.TRANSPORT,
        messengerSDKVersion: String? = null,
        logging: Boolean = false,
        reconnectionTimeoutInSeconds: Long = 60 * 5,
        autoRefreshTokenWhenExpired: Boolean = true,
        encryptedVault: Boolean = false
    ): Configuration {
        require(!(applicationType == ApplicationType.MESSENGER_SDK && messengerSDKVersion == null)) {
            "messengerSDKVersion is required when sdkType is MESSENGER"
        }

        val config = Configuration(
            deploymentId = deploymentId,
            domain = domain,
            logging = logging,
            reconnectionTimeoutInSeconds = reconnectionTimeoutInSeconds,
            autoRefreshTokenWhenExpired = autoRefreshTokenWhenExpired,
            encryptedVault = encryptedVault
        )

        config.application = when (applicationType) {
            ApplicationType.TRANSPORT -> "TransportSDK-${MessengerTransportSDK.sdkVersion}"
            ApplicationType.MESSENGER_SDK -> "MessengerSDK-$messengerSDKVersion/TransportSDK-${MessengerTransportSDK.sdkVersion}"
        }
        return config
    }
}
