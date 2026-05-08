package com.genesys.cloud.messenger.transport.util

internal expect fun getEnvironmentVariable(name: String): String?

internal const val CUSTOM_ENDPOINT_ENV_KEY = "TRANSPORT_CUSTOM_ENDPOINT"
