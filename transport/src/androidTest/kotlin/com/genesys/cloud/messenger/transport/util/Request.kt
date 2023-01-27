package com.genesys.cloud.messenger.transport.util

internal object Request {
    fun configureRequest(startNew: Boolean = false) =
        """{"token":"00000000-0000-0000-0000-000000000000","deploymentId":"deploymentId","startNew":$startNew,"journeyContext":{"customer":{"id":"00000000-0000-0000-0000-000000000000","idType":"cookie"},"customerSession":{"id":"","type":"web"}},"action":"configureSession"}"""
    const val userTypingRequest =
        """{"token":"00000000-0000-0000-0000-000000000000","action":"onMessage","message":{"events":[{"eventType":"Typing","typing":{"type":"On"}}],"type":"Event"}}"""
    const val echoRequest =
        """{"token":"00000000-0000-0000-0000-000000000000","action":"echo","message":{"text":"ping","metadata":{"customMessageId":"SGVhbHRoQ2hlY2tNZXNzYWdlSWQ="},"type":"Text"}}"""
    const val autostart =
        """{"token":"00000000-0000-0000-0000-000000000000","action":"onMessage","message":{"events":[{"eventType":"Presence","presence":{"type":"Join"}}],"type":"Event"}}"""
    const val closeAllConnections =
        """{"token":"00000000-0000-0000-0000-000000000000","closeAllConnections":true,"action":"closeSession"}"""
}
