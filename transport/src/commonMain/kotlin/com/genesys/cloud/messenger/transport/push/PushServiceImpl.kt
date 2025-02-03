package com.genesys.cloud.messenger.transport.push

internal class PushServiceImpl : PushService {

    @Throws(Exception::class)
    override suspend fun synchronize(deviceToken: String, notificationProvider: NotificationProvider) {
        // TODO("Not yet implemented")
    }
}
