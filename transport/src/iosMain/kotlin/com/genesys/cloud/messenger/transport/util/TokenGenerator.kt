package com.genesys.cloud.messenger.transport.util

import platform.Foundation.NSUUID

actual object TokenGenerator {
    actual fun generate() = NSUUID().UUIDString()
}