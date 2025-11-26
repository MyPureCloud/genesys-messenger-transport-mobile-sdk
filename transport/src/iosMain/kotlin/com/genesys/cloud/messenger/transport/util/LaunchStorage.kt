package com.genesys.cloud.messenger.transport.util

import platform.Foundation.NSUserDefaults

internal class LaunchStorage(private val serviceName: String) {
    companion object {
        private const val USER_DEFAULTS_SUFFIX: String = "userDefaults"
    }
    
    private val previouslyLaunched: String = "previouslyLaunched"
    private val userDefaults = NSUserDefaults(suiteName = "$serviceName.$USER_DEFAULTS_SUFFIX")
    
    internal val didLaunchPreviously: Boolean
        get() = userDefaults.boolForKey(previouslyLaunched)

    internal fun markLaunched() {
        userDefaults.setBool(true, previouslyLaunched)
    }
}