package com.genesys.cloud.messenger.journey

/**
 * Configuration required to initialize [JourneyTracker].
 *
 * @param deploymentId the ID of the Genesys Cloud Messenger deployment.
 * @param domain the regional base domain address. For example, "mypurecloud.com".
 * @param logging indicates if logging should be enabled.
 */
data class JourneyConfiguration(
    val deploymentId: String,
    val domain: String,
    val logging: Boolean = false,
)
