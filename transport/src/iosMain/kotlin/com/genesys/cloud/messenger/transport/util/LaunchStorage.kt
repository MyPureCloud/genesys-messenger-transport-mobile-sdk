package com.genesys.cloud.messenger.transport.util

import platform.Foundation.NSUserDefaults

internal class LaunchStorage(
    private val serviceName: String,
    private val userDefaults: NSUserDefaults = NSUserDefaults(suiteName = "$serviceName.$USER_DEFAULTS_SUFFIX")
) {
    private val previouslyLaunched: String = "previouslyLaunched"

    internal val didLaunchPreviously: Boolean
        get() = userDefaults.boolForKey(previouslyLaunched)

    internal fun markLaunched() {
        userDefaults.setBool(true, previouslyLaunched)
    }

    internal fun wasInstalled(keychainChecker: (String) -> Boolean): Boolean {
        return keychainChecker(INSTALL_MARKER_KEY)
    }

    internal fun markInstalled(keychainSetter: (String, String) -> Unit) {
        keychainSetter(INSTALL_MARKER_KEY, "true")
    }

    companion object {
        private const val USER_DEFAULTS_SUFFIX: String = "userDefaults"
        private const val INSTALL_MARKER_KEY: String = "installMarker"
    }
}
