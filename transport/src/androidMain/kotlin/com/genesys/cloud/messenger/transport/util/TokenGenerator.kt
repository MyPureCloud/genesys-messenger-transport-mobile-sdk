package com.genesys.cloud.messenger.transport.util

import java.util.*

actual object TokenGenerator {
    actual fun generate() = UUID.randomUUID().toString()
}