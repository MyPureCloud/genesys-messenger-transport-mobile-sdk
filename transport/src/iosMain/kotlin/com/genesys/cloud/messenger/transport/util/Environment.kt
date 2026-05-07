package com.genesys.cloud.messenger.transport.util

import platform.Foundation.NSProcessInfo

internal actual fun getEnvironmentVariable(name: String): String? =
    NSProcessInfo.processInfo.environment[name] as? String
