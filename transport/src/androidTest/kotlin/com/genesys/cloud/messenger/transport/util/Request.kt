package com.genesys.cloud.messenger.transport.util

internal object Request {
    const val token = "00000000-0000-0000-0000-000000000000"
    fun configureRequest(startNew: Boolean = false) =
        """{"token":"$token","deploymentId":"deploymentId","startNew":$startNew,"journeyContext":{"customer":{"id":"00000000-0000-0000-0000-000000000000","idType":"cookie"},"customerSession":{"id":"","type":"web"}},"action":"configureSession"}"""
    fun configureAuthenticatedRequest(startNew: Boolean) =
        """{"token":"$token","deploymentId":"deploymentId","startNew":$startNew,"journeyContext":{"customer":{"id":"00000000-0000-0000-0000-000000000000","idType":"cookie"},"customerSession":{"id":"","type":"web"}},"data":{"code":"auth_token"},"action":"configureAuthenticatedSession"}"""
    const val userTypingRequest =
        """{"token":"$token","action":"onMessage","message":{"events":[{"eventType":"Typing","typing":{"type":"On"}}],"type":"Event"}}"""
    const val echoRequest =
        """{"token":"$token","action":"echo","message":{"text":"ping","metadata":{"customMessageId":"SGVhbHRoQ2hlY2tNZXNzYWdlSWQ="},"type":"Text"}}"""
    const val autostart =
        """{"token":"$token","action":"onMessage","message":{"events":[{"eventType":"Presence","presence":{"type":"Join"}}],"type":"Event"}}"""
    const val closeAllConnections =
        """{"token":"$token","closeAllConnections":true,"action":"closeSession"}"""
}
