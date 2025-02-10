package com.genesys.cloud.messenger.transport.push

import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.Vault
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages

internal class PushServiceImpl(
    private val vault: Vault,
    private val platform: Platform = Platform(),
    private val pushConfigComparator: PushConfigComparator = PushConfigComparatorImpl(),
    private val log: Log,
) : PushService {

    @Throws(Exception::class)
    override suspend fun synchronize(deviceToken: String, pushProvider: PushProvider) {
        log.i { LogMessages.synchronizingPush(deviceToken, pushProvider) }
        val storedPushConfig = vault.pushConfig
        val userPushConfig = buildPushConfigFromUserData(deviceToken, pushProvider)
        val diff = pushConfigComparator.compare(userPushConfig, storedPushConfig)
        // TODO("Not yet implemented. MTSDK-528")
    }

    private fun buildPushConfigFromUserData(
        deviceToken: String,
        pushProvider: PushProvider,
    ): PushConfig {
        return PushConfig(
            token = vault.token,
            deviceToken = deviceToken,
            preferredLanguage = platform.preferredLanguage(),
            lastSyncTimestamp = platform.epochMillis(),
            deviceType = platform.os,
            pushProvider = pushProvider,
        )
    }
}
