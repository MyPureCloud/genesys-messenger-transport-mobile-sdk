package com.genesys.cloud.messenger.composeapp.model

/**
 * Represents deployment configuration settings for the testbed.
 *
 * @param deploymentId The deployment ID for the messaging client
 * @param region The region/domain for the messaging client
 */
data class AppSettings(
    val deploymentId: String = "",
    val region: String = ""
)