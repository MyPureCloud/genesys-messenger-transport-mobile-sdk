package com.genesys.cloud.messenger.transport.push

import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.Result
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.push.DeviceTokenOperation.Delete
import com.genesys.cloud.messenger.transport.push.DeviceTokenOperation.Register
import com.genesys.cloud.messenger.transport.push.DeviceTokenOperation.Update
import com.genesys.cloud.messenger.transport.push.PushConfigComparator.Diff
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.UNKNOWN
import com.genesys.cloud.messenger.transport.util.Vault
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

internal class PushServiceImpl(
    private val vault: Vault,
    private val api: WebMessagingApi,
    private val platform: Platform = Platform(),
    private val pushConfigComparator: PushConfigComparator = PushConfigComparatorImpl(),
    private val log: Log,
) : PushService {

    @Throws(DeviceTokenException::class, IllegalArgumentException::class, CancellationException::class)
    override suspend fun synchronize(
        deviceToken: String,
        pushProvider: PushProvider
    ) {
        log.i { LogMessages.synchronizingPush(deviceToken, pushProvider) }
        val storedPushConfig = vault.pushConfig
        val userPushConfig = buildPushConfigFromUserData(deviceToken, pushProvider)
        val diff = pushConfigComparator.compare(userPushConfig, storedPushConfig)
        log.i { LogMessages.pushDiff(diff) }
        handleDiff(diff, userPushConfig, storedPushConfig)
    }

    @Throws(DeviceTokenException::class, CancellationException::class)
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

    @Throws(DeviceTokenException::class, CancellationException::class)
    private suspend fun handleDiff(
        diff: Diff,
        userPushConfig: PushConfig,
        storedPushConfig: PushConfig
    ) {
        when (diff) {
            Diff.NONE -> log.i { LogMessages.deviceTokenIsInSync(userPushConfig) }

            Diff.NO_TOKEN -> register(userPushConfig)
            Diff.TOKEN -> {
                coroutineScope {
                    launch { register(userPushConfig) }
                }
            }

            Diff.DEVICE_TOKEN,
            Diff.LANGUAGE,
            Diff.EXPIRED,
            -> update(userPushConfig)
        }
    }

    @Throws(DeviceTokenException::class, CancellationException::class)
    private suspend fun register(userPushConfig: PushConfig) {
        when (val result = api.performDeviceTokenOperation(userPushConfig, Register)) {
            is Result.Success -> {
                log.i { LogMessages.deviceTokenWasRegistered(userPushConfig) }
                vault.pushConfig = userPushConfig
            }

            is Result.Failure -> handleRequestError(result, userPushConfig, Register)
        }
    }

    @Throws(DeviceTokenException::class, CancellationException::class)
    private suspend fun update(userPushConfig: PushConfig) {
        when (val result = api.performDeviceTokenOperation(userPushConfig, Update)) {
            is Result.Success -> {
                log.i { LogMessages.deviceTokenWasUpdated(userPushConfig) }
                vault.pushConfig = userPushConfig
            }

            is Result.Failure -> handleRequestError(result, userPushConfig, Update)
        }
    }

    @Throws(DeviceTokenException::class, CancellationException::class)
    private suspend fun delete(
        pushConfig: PushConfig,
        clearStoredPushConfigUponSuccess: Boolean = false,
    ) {
        when (val result = api.performDeviceTokenOperation(pushConfig, Delete)) {
            is Result.Success -> handleSuccessForDeleteOperation(pushConfig, clearStoredPushConfigUponSuccess)

            is Result.Failure -> if (result.errorCode == ErrorCode.DeviceNotFound) {
                handleSuccessForDeleteOperation(pushConfig, clearStoredPushConfigUponSuccess)
            } else {
                handleRequestError(result, pushConfig, Delete)
            }
        }
    }

    @Throws(DeviceTokenException::class, CancellationException::class)
    private suspend fun handleRequestError(
        result: Result.Failure,
        userPushConfig: PushConfig,
        operation: DeviceTokenOperation,
    ) {
        when (result.errorCode) {
            ErrorCode.CancellationError -> {
                log.w { LogMessages.cancellationExceptionRequestName("DeviceToken.$operation") }
                result.run {
                    throw throwable ?: DeviceTokenException(errorCode, message, throwable)
                }
            }

            ErrorCode.DeviceNotFound -> {
                if (operation == Update) {
                    log.i { LogMessages.DEVICE_NOT_REGISTERED }
                    register(userPushConfig)
                } else {
                    throwDeviceTokenException(result, userPushConfig)
                }
            }

            ErrorCode.DeviceAlreadyRegistered -> {
                log.i { LogMessages.DEVICE_ALREADY_REGISTERED }
                vault.pushConfig = userPushConfig
                update(userPushConfig)
            }

            else -> throwDeviceTokenException(result, userPushConfig)
        }
    }

    @Throws(DeviceTokenException::class)
    private fun throwDeviceTokenException(
        result: Result.Failure,
        userPushConfig: PushConfig
    ) {
        log.e { LogMessages.failedToSynchronizeDeviceToken(userPushConfig, result.errorCode) }
        throw DeviceTokenException(result.errorCode, result.message, result.throwable)
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

    private fun handleSuccessForDeleteOperation(
        pushConfig: PushConfig,
        clearStoredPushConfigUponSuccess: Boolean
    ) {
        log.i { LogMessages.deviceTokenWasDeleted(pushConfig) }
        if (clearStoredPushConfigUponSuccess) vault.remove(vault.keys.pushConfigKey)
    }
}
