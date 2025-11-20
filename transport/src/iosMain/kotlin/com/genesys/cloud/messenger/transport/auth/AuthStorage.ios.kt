package com.genesys.cloud.messenger.transport.auth

import platform.Foundation.NSUserDefaults

actual class AuthStorage {
    private val userDefaults = NSUserDefaults.standardUserDefaults
    actual val previouslyAuthorized: Boolean
        get() = userDefaults.boolForKey("previouslyAuthorized")

    actual fun markAuthorized() {
        userDefaults.setBool(true, "previouslyAuthorized")
    }
}