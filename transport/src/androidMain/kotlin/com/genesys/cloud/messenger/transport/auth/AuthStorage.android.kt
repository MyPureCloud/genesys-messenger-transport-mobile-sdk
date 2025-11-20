package com.genesys.cloud.messenger.transport.auth

import android.content.Context

actual class AuthStorage {

    actual val previouslyAuthorized: Boolean = true
    actual fun markAuthorized() { }
}