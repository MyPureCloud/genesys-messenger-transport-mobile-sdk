package com.genesys.cloud.messenger.transport.util

import platform.Foundation.NSUserDefaults

internal class LaunchStorage {
    private val userDefaults = NSUserDefaults(suiteName = "com.transportsdk.internal.storage")
    private val previouslyLaunched: String = "previouslyLaunched"
    internal val didLaunchPreviously: Boolean
        get() = userDefaults.boolForKey(previouslyLaunched)

    internal fun markLaunched() {
        userDefaults.setBool(true, previouslyLaunched)
    }
}