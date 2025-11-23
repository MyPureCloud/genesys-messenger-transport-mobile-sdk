package com.genesys.cloud.messenger.transport.network.test_engines

/**
 * Mock exception for testing network error scenarios.
 * This is a common exception that will be wrapped by platform-specific network exceptions in actual tests.
 * For Android tests, wrap this with UnknownHostException.
 * For iOS tests, this will be wrapped appropriately.
 */
internal class NetworkExceptionForTesting(message: String = "Mock network error") : Exception(message)
