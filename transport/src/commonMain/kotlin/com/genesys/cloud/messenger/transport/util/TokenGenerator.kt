package com.genesys.cloud.messenger.transport.util

expect object TokenGenerator {
    fun generate(): String
}