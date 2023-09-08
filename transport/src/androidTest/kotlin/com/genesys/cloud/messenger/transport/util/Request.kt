package com.genesys.cloud.messenger.transport.util

import com.genesys.cloud.messenger.transport.utility.AuthTest

internal object Request {
    const val token = "00000000-0000-0000-0000-000000000000"
    fun configureRequest(startNew: Boolean = false) =
        """{"token":"$token","deploymentId":"deploymentId","startNew":$startNew,"journeyContext":{"customer":{"id":"00000000-0000-0000-0000-000000000000","idType":"cookie"},"customerSession":{"id":"","type":"web"}},"action":"configureSession"}"""
    fun configureAuthenticatedRequest(startNew: Boolean = false) =
        """{"token":"$token","deploymentId":"deploymentId","startNew":$startNew,"journeyContext":{"customer":{"id":"00000000-0000-0000-0000-000000000000","idType":"cookie"},"customerSession":{"id":"","type":"web"}},"data":{"code":"${AuthTest.JwtToken}"},"action":"configureAuthenticatedSession"}"""
    const val userTypingRequest =
        """{"token":"$token","action":"onMessage","message":{"events":[{"eventType":"Typing","typing":{"type":"On"}}],"type":"Event"}}"""
    const val echoRequest =
        """{"token":"$token","action":"echo","message":{"text":"ping","metadata":{"customMessageId":"SGVhbHRoQ2hlY2tNZXNzYWdlSWQ="},"type":"Text"}}"""
    fun autostart(channelWithCustomAttributes: String = """"channel":{"metadata":{"customAttributes":{"A":"B"}}},""") =
        """{"token":"$token","action":"onMessage","message":{"events":[{"eventType":"Presence","presence":{"type":"Join"}}],$channelWithCustomAttributes"type":"Event"}}"""
    const val closeAllConnections =
        """{"token":"$token","closeAllConnections":true,"action":"closeSession"}"""
    const val clearConversation =
        """{"token":"$token","action":"onMessage","message":{"events":[{"eventType":"Presence","presence":{"type":"Clear"}}],"type":"Event"}}"""
}
