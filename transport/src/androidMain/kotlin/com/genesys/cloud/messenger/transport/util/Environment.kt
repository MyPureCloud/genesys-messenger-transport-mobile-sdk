package com.genesys.cloud.messenger.transport.util

internal actual fun getEnvironmentVariable(name: String): String? =
    System.getenv(name)
