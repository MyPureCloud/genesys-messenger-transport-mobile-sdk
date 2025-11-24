package com.genesys.cloud.messenger.transport.auth

actual class AuthStorage {

    actual val previouslyAuthorized: Boolean = true
    actual fun markAuthorized() { }
}