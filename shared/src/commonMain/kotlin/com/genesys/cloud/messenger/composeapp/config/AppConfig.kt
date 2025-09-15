package com.genesys.cloud.messenger.composeapp.config

/**
 * Application configuration constants and default values.
 * This replaces BuildConfig functionality for multiplatform usage.
 */
object AppConfig {
    
    /**
     * Default deployment ID from deployment.properties
     */
    const val DEFAULT_DEPLOYMENT_ID = "00c966c5-8f88-42b5-ae9b-fa81b5721569"
    
    /**
     * Default region/domain from deployment.properties
     */
    const val DEFAULT_REGION = "inindca.com"
    
    /**
     * Available regions for the messaging client
     */
    val AVAILABLE_REGIONS = listOf(
        "inindca.com",
        "inintca.com", 
        "mypurecloud.com",
        "usw2.pure.cloud",
        "mypurecloud.jp",
        "mypurecloud.com.au",
        "mypurecloud.de",
        "euw2.pure.cloud",
        "cac1.pure.cloud",
        "apne2.pure.cloud",
        "aps1.pure.cloud",
        "sae1.pure.cloud",
        "mec1.pure.cloud",
        "apne3.pure.cloud",
        "euc2.pure.cloud"
    )
}