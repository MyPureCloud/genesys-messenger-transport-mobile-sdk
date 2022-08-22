package com.genesys.cloud.messenger.transport.util

internal object Request {
    const val configureRequest =
        """{"token":"00000000-0000-0000-0000-000000000000","deploymentId":"deploymentId","journeyContext":{"customer":{"id":"00000000-0000-0000-0000-000000000000","idType":"cookie"},"customerSession":{"id":"","type":"web"}},"action":"configureSession"}"""
    const val userTypingRequest =
        """{"token":"00000000-0000-0000-0000-000000000000","action":"onMessage","message":{"events":[{"eventType":"Typing","typing":{"type":"On","duration":5000}}],"type":"Event"}}"""
}
