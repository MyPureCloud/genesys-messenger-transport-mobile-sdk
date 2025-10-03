package transport.util

import com.genesys.cloud.messenger.transport.shyrka.send.HealthCheckID
import com.genesys.cloud.messenger.transport.utility.AuthTest
import com.genesys.cloud.messenger.transport.utility.MessageValues

internal object Request {
    const val token = "00000000-0000-0000-0000-000000000000"

    fun configureRequest(startNew: Boolean = false) =
        """{"token":"$token","deploymentId":"deploymentId","startNew":$startNew,"journeyContext":{"customer":{"id":"00000000-0000-0000-0000-000000000000","idType":"cookie"},"customerSession":{"id":"","type":"web"}},"tracingId":"test-tracing-id","action":"configureSession"}"""

    fun configureAuthenticatedRequest(startNew: Boolean = false) =
        """{"token":"$token","deploymentId":"deploymentId","startNew":$startNew,"journeyContext":{"customer":{"id":"00000000-0000-0000-0000-000000000000","idType":"cookie"},"customerSession":{"id":"","type":"web"}},"data":{"code":"${AuthTest.JWT_TOKEN}"},"tracingId":"test-tracing-id","action":"configureAuthenticatedSession"}"""

    const val userTypingRequest =
        """{"token":"$token","tracingId":"test-tracing-id","action":"onMessage","message":{"events":[{"eventType":"Typing","typing":{"type":"On"}}],"type":"Event"}}"""
    const val echo =
        """{"token":"$token","tracingId":"$HealthCheckID","action":"echo","message":{"text":"ping","type":"Text"}}"""

    fun autostart(channelWithCustomAttributes: String = """"channel":{"metadata":{"customAttributes":{"A":"B"}}},""") =
        """{"token":"$token","tracingId":"test-tracing-id","action":"onMessage","message":{"events":[{"eventType":"Presence","presence":{"type":"Join"}}],$channelWithCustomAttributes"type":"Event"}}"""

    fun quickReplyWith(
        content: String = """"content":[{"contentType":"ButtonResponse","buttonResponse":{"text":"text_a","payload":"payload_a","type":"QuickReply"}}]""",
        channel: String = ""
    ) = """{"token":"$token","tracingId":"test-tracing-id","message":{"text":"",$content,$channel"type":"Text"},"action":"onMessage"}"""

    fun textMessage(text: String = MessageValues.TEXT) =
        """{"token":"$token","tracingId":"test-tracing-id","message":{"text":"$text","type":"Text"},"action":"onMessage"}"""

    const val closeAllConnections =
        """{"token":"$token","closeAllConnections":true,"tracingId":"test-tracing-id","action":"closeSession"}"""

    fun clearConversation() =
        """{"token":"$token","tracingId":"test-tracing-id","action":"onMessage","message":{"events":[{"eventType":"Presence","presence":{"type":"Clear"}}],"type":"Event"}}"""

    const val jwt = """{"token":"00000000-0000-0000-0000-000000000000","tracingId":"test-tracing-id","action":"getJwt"}"""
    const val refreshAttachmentUrl =
        """{"token":"$token","attachmentId":"88888888-8888-8888-8888-888888888888","tracingId":"test-tracing-id","action":"getAttachment"}"""
}
