package com.genesys.cloud.messenger.transport.push

import kotlin.coroutines.cancellation.CancellationException

/**
 * Service interface for push notification operations.
 */
interface PushService {

    /**
     * Synchronizes the device token with the specified notification provider.
     *
     * @param deviceToken The device token for push notifications.
     * @param pushProvider The type of notification provider to use.
     *
     * @throws DeviceTokenException  if synchronization fails due to server or network issues.
     * @throws IllegalArgumentException if deviceToken or pushProvider is invalid.
     * @throws CancellationException if the coroutine is cancelled.
     */
    @Throws(DeviceTokenException::class, IllegalArgumentException::class, CancellationException::class)
    suspend fun synchronize(deviceToken: String, pushProvider: PushProvider)

    /**
     * Unregister device from push notifications.
     *
     * @throws DeviceTokenException  if synchronization fails due to server or network issues.
     * @throws CancellationException if the coroutine is cancelled.
     */
    @Throws(DeviceTokenException::class, CancellationException::class)
    suspend fun unregister()
}
