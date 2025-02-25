package com.genesys.cloud.messenger.transport.push

import com.genesys.cloud.messenger.transport.core.Result
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.push.PushConfigComparator.Diff
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.UNKNOWN
import com.genesys.cloud.messenger.transport.util.Vault
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal class PushServiceImpl(
    private val vault: Vault,
    private val api: WebMessagingApi,
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
        handleDiff(diff, userPushConfig)
    }

    override suspend fun unregister() {
        log.i { LogMessages.UNREGISTERING_DEVICE }
        vault.pushConfig.run {
            if (token == UNKNOWN) {
                log.i { LogMessages.DEVICE_NOT_REGISTERED }
                return
            }
            delete(this, true)
        }
    }

    private suspend fun handleDiff(diff: Diff, userPushConfig: PushConfig) {
        when (diff) {
            Diff.NONE -> log.i { LogMessages.deviceTokenIsInSync(userPushConfig) }

            Diff.NO_TOKEN -> register(userPushConfig)
            Diff.TOKEN -> {
                coroutineScope {
                    launch { delete(userPushConfig) }
                    launch { register(userPushConfig) }
                }
            }

            Diff.DEVICE_TOKEN,
            Diff.LANGUAGE,
            Diff.EXPIRED,
            -> update(userPushConfig)
        }
    }

    private suspend fun register(userPushConfig: PushConfig) {
        val result = api.registerDeviceToken(userPushConfig)
        when (result) {
            is Result.Success -> {
                log.i { LogMessages.deviceTokenWasRegistered(userPushConfig) }
                vault.pushConfig = userPushConfig
            }

            is Result.Failure -> TODO("Not yet implemented: MTSDK-416")
        }
    }

    private suspend fun update(userPushConfig: PushConfig) {
        val result = api.updateDeviceToken(userPushConfig)
        when (result) {
            is Result.Success -> {
                log.i { LogMessages.deviceTokenWasUpdated(userPushConfig) }
                vault.pushConfig = userPushConfig
            }

            is Result.Failure -> TODO("Not yet implemented: MTSDK-416")
        }
    }

    private suspend fun delete(
        userPushConfig: PushConfig,
        clearStoredPushConfigUponSuccess: Boolean = false,
    ) {
        val result = api.deleteDeviceToken(userPushConfig)
        when (result) {
            is Result.Success -> {
                log.i { LogMessages.deviceTokenWasDeleted(userPushConfig) }
                if (clearStoredPushConfigUponSuccess) vault.remove(vault.keys.pushConfigKey)
            }

            is Result.Failure -> TODO("Not yet implemented: MTSDK-416")
        }
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
            deviceType = platform.os.lowercase(),
            pushProvider = pushProvider,
        )
    }
}
