package com.genesys.cloud.messenger.transport.push

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
     * @throws Exception if synchronization fails.
     */
    @Throws(Exception::class)
    suspend fun synchronize(deviceToken: String, pushProvider: PushProvider)

    /**
     * Unregister device from push notifications.
     *
     * @throws Exception if unregister fails.
     */
    @Throws(Exception::class)
    suspend fun unregister()
}
