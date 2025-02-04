package com.genesys.cloud.messenger.transport.push

/**
 * Represents push notification providers for mobile platforms.
 *
 * @property APNS Apple Push Notification Service.
 * @property FCM Firebase Cloud Messaging.
 */
enum class NotificationProvider {
    APNS,
    FCM,
}
