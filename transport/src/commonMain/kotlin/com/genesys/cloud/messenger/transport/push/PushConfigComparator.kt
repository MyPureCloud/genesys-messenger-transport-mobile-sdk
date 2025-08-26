package com.genesys.cloud.messenger.transport.push

import com.genesys.cloud.messenger.transport.core.ErrorMessage
import com.genesys.cloud.messenger.transport.push.PushConfigComparator.Diff
import com.genesys.cloud.messenger.transport.util.UNKNOWN

internal interface PushConfigComparator {
    @Throws(IllegalArgumentException::class)
    fun compare(userConfig: PushConfig, storedConfig: PushConfig): Diff

    enum class Diff {
        NONE,
        NO_TOKEN,
        TOKEN,
        DEVICE_TOKEN,
        LANGUAGE,
        EXPIRED,
    }
}

internal class PushConfigComparatorImpl : PushConfigComparator {

    @Throws(IllegalArgumentException::class)
    override fun compare(userConfig: PushConfig, storedConfig: PushConfig): Diff = userConfig.run {
        if (deviceToken.isEmpty()) throw IllegalArgumentException(ErrorMessage.INVALID_DEVICE_TOKEN)
        if (pushProvider == null) throw IllegalArgumentException(ErrorMessage.INVALID_PUSH_PROVIDER)
        when {
            this == storedConfig -> Diff.NONE
            storedConfig.token == UNKNOWN -> Diff.NO_TOKEN
            token != storedConfig.token -> Diff.TOKEN
            deviceToken != storedConfig.deviceToken -> Diff.DEVICE_TOKEN
            preferredLanguage != storedConfig.preferredLanguage -> Diff.LANGUAGE
            lastSyncTimestamp - storedConfig.lastSyncTimestamp >= DEVICE_TOKEN_EXPIRATION_IN_MILLISECONDS -> Diff.EXPIRED
            else -> Diff.NONE
        }
    }
}
