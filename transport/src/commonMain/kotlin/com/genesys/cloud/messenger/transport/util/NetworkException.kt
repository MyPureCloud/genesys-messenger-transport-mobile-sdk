package com.genesys.cloud.messenger.transport.util

/**
 * Checks if an exception is caused by network connectivity issues.
 * Platform-specific implementations will check for appropriate exception types.
 */
internal expect fun Exception.isNetworkException(): Boolean

