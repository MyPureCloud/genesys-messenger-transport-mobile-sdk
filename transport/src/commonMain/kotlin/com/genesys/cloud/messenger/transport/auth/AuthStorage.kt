package com.genesys.cloud.messenger.transport.auth

expect class AuthStorage() {
    val previouslyAuthorized: Boolean
    fun markAuthorized()
}