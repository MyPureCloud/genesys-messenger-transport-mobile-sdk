package transport.util

import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.send.ConfigureAuthenticatedSessionRequest
import com.genesys.cloud.messenger.transport.shyrka.send.ConfigureSessionRequest
import com.genesys.cloud.messenger.transport.shyrka.send.HealthCheckID
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyContext
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyCustomer
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyCustomerSession
import com.genesys.cloud.messenger.transport.utility.AuthTest
import com.genesys.cloud.messenger.transport.utility.MessageValues
import com.genesys.cloud.messenger.transport.utility.TestValues

internal object Request {
    const val token = "00000000-0000-0000-0000-000000000000"

    fun configureRequest(startNew: Boolean = false): String {
        val request = ConfigureSessionRequest(
            token = token,
            deploymentId = "deploymentId",
            startNew = startNew,
            journeyContext = JourneyContext(
                JourneyCustomer("00000000-0000-0000-0000-000000000000", "cookie"),
                JourneyCustomerSession("", "web")
            ),
            tracingId = TestValues.TRACING_ID
        )
        return WebMessagingJson.json.encodeToString(request)
    }

    fun configureAuthenticatedRequest(startNew: Boolean = false): String {
        val request = ConfigureAuthenticatedSessionRequest(
            token = token,
            deploymentId = "deploymentId",
            startNew = startNew,
            journeyContext = JourneyContext(
                JourneyCustomer("00000000-0000-0000-0000-000000000000", "cookie"),
                JourneyCustomerSession("", "web")
            ),
            data = ConfigureAuthenticatedSessionRequest.Data(AuthTest.JWT_TOKEN),
            tracingId = TestValues.TRACING_ID
        )
        return WebMessagingJson.json.encodeToString(request)
    }

    const val userTypingRequest =
        """{"token":"$token","tracingId":"${TestValues.TRACING_ID}","action":"onMessage","message":{"events":[{"eventType":"Typing","typing":{"type":"On"}}],"type":"Event"}}"""
    const val echo =
        """{"token":"$token","tracingId":"$HealthCheckID","action":"echo","message":{"text":"ping","type":"Text"}}"""

    fun autostart(channelWithCustomAttributes: String = """"channel":{"metadata":{"customAttributes":{"A":"B"}}},""") = """{"token":"$token","tracingId":"${TestValues.TRACING_ID}","action":"onMessage","message":{"events":[{"eventType":"Presence","presence":{"type":"Join"}}],$channelWithCustomAttributes"type":"Event"}}"""

    fun quickReplyWith(
        content: String = """"content":[{"contentType":"ButtonResponse","buttonResponse":{"text":"text_a","payload":"payload_a","type":"QuickReply"}}]""",
        channel: String = ""
    ) = """{"token":"$token","message":{"text":"",$content${if (channel.isNotEmpty()) ",$channel" else ""},"type":"Text"},"tracingId":"${TestValues.TRACING_ID}","action":"onMessage"}"""

    fun textMessage(text: String = MessageValues.TEXT) = """{"token":"$token","message":{"text":"$text","type":"Text"},"tracingId":"${TestValues.TRACING_ID}","action":"onMessage"}"""

    const val closeAllConnections =
        """{"token":"$token","closeAllConnections":true,"tracingId":"${TestValues.TRACING_ID}","action":"closeSession"}"""

    fun clearConversation() = """{"token":"$token","tracingId":"${TestValues.TRACING_ID}","action":"onMessage","message":{"events":[{"eventType":"Presence","presence":{"type":"Clear"}}],"type":"Event"}}"""

    const val jwt = """{"token":"00000000-0000-0000-0000-000000000000","tracingId":"${TestValues.TRACING_ID}","action":"getJwt"}"""
    const val refreshAttachmentUrl =
        """{"token":"$token","attachmentId":"88888888-8888-8888-8888-888888888888","tracingId":"${TestValues.TRACING_ID}","action":"getAttachment"}"""
}
