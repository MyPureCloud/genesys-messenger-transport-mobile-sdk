package com.genesys.cloud.messenger.transport.util

expect class Platform() {
    val platform: String

    fun randomUUID(): String

    fun epochMillis(): Long
}
