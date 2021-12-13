package com.genesys.cloud.messenger.transport.shyrka.send

object TestShyrkaSendMessages {
    fun configureMessage(
        token: String = "00000000-0000-0000-0000-000000000000",
        deploymentId: String = "deploymentId",
        customerId: String = "00000000-0000-0000-0000-000000000000",
        customerIdType: String = "cookie",
        customerSessionId: String = "",
        customerSessionType: String = "web"
    ) =
        """{"token":"$token","deploymentId":"$deploymentId","journeyContext":{"customer":{"id":"$customerId","idType":"$customerIdType"},"customerSession":{"id":"$customerSessionId","type":"$customerSessionType"}},"action":"configureSession"}"""
}
