package transport.util

import com.genesys.cloud.messenger.transport.shyrka.send.HealthCheckID
import com.genesys.cloud.messenger.transport.utility.AuthTest
import com.genesys.cloud.messenger.transport.utility.MessageValues
import com.genesys.cloud.messenger.transport.utility.TestValues

internal object Request {
    const val token = "00000000-0000-0000-0000-000000000000"
    fun configureRequest(startNew: Boolean = false) =
        """{"token":"$token","deploymentId":"deploymentId","startNew":$startNew,"journeyContext":{"customer":{"id":"00000000-0000-0000-0000-000000000000","idType":"cookie"},"customerSession":{"id":"","type":"web"}},"action":"configureSession"}"""
    fun configureAuthenticatedRequest(startNew: Boolean = false) =
        """{"token":"$token","deploymentId":"deploymentId","startNew":$startNew,"journeyContext":{"customer":{"id":"00000000-0000-0000-0000-000000000000","idType":"cookie"},"customerSession":{"id":"","type":"web"}},"data":{"code":"${AuthTest.JWT_TOKEN}"},"action":"configureAuthenticatedSession"}"""
    const val userTypingRequest =
        """{"token":"$token","action":"onMessage","message":{"events":[{"eventType":"Typing","typing":{"type":"On"}}],"type":"Event"}}"""
    const val echo =
        """{"token":"$token","action":"echo","message":{"text":"ping","metadata":{"customMessageId":"$HealthCheckID"},"type":"Text"}}"""
    fun autostart(channelWithCustomAttributes: String = """"channel":{"metadata":{"customAttributes":{"A":"B"}}},""") =
        """{"token":"$token","action":"onMessage","message":{"events":[{"eventType":"Presence","presence":{"type":"Join"}}],$channelWithCustomAttributes"type":"Event"}}"""
    fun quickReplyWith(
        content: String = """"content":[{"contentType":"ButtonResponse","buttonResponse":{"text":"text_a","payload":"payload_a","type":"QuickReply"}}]""",
        channel: String = ""
    ) = """{"token":"$token","message":{"text":"",$content,$channel"type":"Text"},"action":"onMessage"}"""
    fun textMessage(text: String = MessageValues.TEXT) =
        """{"token":"$token","message":{"text":"$text","type":"Text"},"action":"onMessage"}"""
    private fun postbackContent(text: String, payload: String, type: String): String =
        """"content":[{"contentType":"ButtonResponse","buttonResponse":{"text":"$text","payload":"$payload","type":"$type"}}]"""
    fun postbackWith(): String =
        """{
        "action": "onMessage",
        "token": "${TestValues.TOKEN}",
        "message": {
            "text": "Book Now",
            "metadata": { "customMessageId": "msg_id" },
            "channel": null,
            "content": []
        }
    }"""
    const val closeAllConnections =
        """{"token":"$token","closeAllConnections":true,"action":"closeSession"}"""
    const val clearConversation =
        """{"token":"$token","action":"onMessage","message":{"events":[{"eventType":"Presence","presence":{"type":"Clear"}}],"type":"Event"}}"""
    const val jwt = """{"token":"00000000-0000-0000-0000-000000000000","action":"getJwt"}"""
    const val refreshAttachmentUrl =
        """{"token":"$token","attachmentId":"88888888-8888-8888-8888-888888888888","action":"getAttachment"}"""
}
