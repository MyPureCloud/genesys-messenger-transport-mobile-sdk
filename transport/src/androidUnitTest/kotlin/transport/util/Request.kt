package transport.util

import com.genesys.cloud.messenger.transport.shyrka.send.HealthCheckID
import com.genesys.cloud.messenger.transport.utility.AuthTest
import com.genesys.cloud.messenger.transport.utility.MessageValues

internal object Request {
    const val token = "00000000-0000-0000-0000-000000000000"

    fun configureRequest(startNew: Boolean = false) = """{"token":"$token","deploymentId":"deploymentId","startNew":$startNew,"journeyContext":{"customer":{"id":"00000000-0000-0000-0000-000000000000","idType":"cookie"},"customerSession":{"id":"","type":"web"}},"action":"configureSession"}"""

    fun isConfigureRequest(json: String, startNew: Boolean = false) =
        json.contains(""""token":"$token"""") &&
            json.contains(""""deploymentId":"deploymentId"""") &&
            json.contains(""""startNew":$startNew""") &&
            json.contains(""""action":"configureSession"""")

    fun configureAuthenticatedRequest(startNew: Boolean = false) = """{"token":"$token","deploymentId":"deploymentId","startNew":$startNew,"journeyContext":{"customer":{"id":"00000000-0000-0000-0000-000000000000","idType":"cookie"},"customerSession":{"id":"","type":"web"}},"data":{"code":"${AuthTest.JWT_TOKEN}"},"action":"configureAuthenticatedSession"}"""

    fun isConfigureAuthenticatedRequest(json: String, startNew: Boolean = false) =
        json.contains(""""token":"$token"""") &&
            json.contains(""""deploymentId":"deploymentId"""") &&
            json.contains(""""startNew":$startNew""") &&
            json.contains(""""data":{"code":"${AuthTest.JWT_TOKEN}"}""") &&
            json.contains(""""action":"configureAuthenticatedSession"""")

    fun isUserTypingRequest(json: String) =
        json.contains(""""token":"$token"""") &&
            json.contains(""""action":"onMessage"""") &&
            json.contains(""""eventType":"Typing"""") &&
            json.contains(""""type":"On"""")

    fun isEchoRequest(json: String) =
        json.contains(""""token":"$token"""") &&
            json.contains(""""tracingId":"$HealthCheckID"""") &&
            json.contains(""""action":"echo"""") &&
            json.contains(""""text":"ping"""")

    fun autostart(channelWithCustomAttributes: String = """"channel":{"metadata":{"customAttributes":{"A":"B"}}},""") = """{"token":"$token","action":"onMessage","message":{"events":[{"eventType":"Presence","presence":{"type":"Join"}}],$channelWithCustomAttributes"type":"Event"}}"""

    fun quickReplyWith(
        content: String = """"content":[{"contentType":"ButtonResponse","buttonResponse":{"text":"text_a","payload":"payload_a","type":"QuickReply"}}]""",
        channel: String = ""
    ) = """{"token":"$token","message":{"text":"",$content,$channel"type":"Text"},"action":"onMessage"}"""

    fun textMessage(text: String = MessageValues.TEXT) = """{"token":"$token","message":{"text":"$text","type":"Text"},"action":"onMessage"}"""

    const val closeAllConnections =
        """{"token":"$token","closeAllConnections":true,"action":"closeSession"}"""
    const val clearConversation =
        """{"token":"$token","action":"onMessage","message":{"events":[{"eventType":"Presence","presence":{"type":"Clear"}}],"type":"Event"}}"""

    fun isJwtRequest(json: String) = json.contains(""""token":"$token"""") && json.contains(""""action":"getJwt"""")

    const val refreshAttachmentUrl =
        """{"token":"$token","attachmentId":"88888888-8888-8888-8888-888888888888","action":"getAttachment"}"""
    val expectedPostbackRequestJson =
        """{"token":"$token","message":{"text":"Postback button text","metadata":{"customMessageId":"card-123"},"content":[{"contentType":"ButtonResponse","buttonResponse":{"text":"Postback button text","payload":"some_payload_value","type":"Postback"}}],"type":"Structured"},"action":"onMessage"}"""

    fun isPostbackRequest(json: String) =
        json.contains(""""token":"$token"""") &&
            json.contains(""""text":"Postback button text"""") &&
            json.contains(""""type":"Structured"""") &&
            json.contains(""""action":"onMessage"""")

    fun isOnMessageRequest(json: String) =
        json.contains(""""token":"$token"""") &&
            json.contains(""""action":"onMessage"""")

    fun isTextMessageRequest(json: String, text: String = MessageValues.TEXT) =
        json.contains(""""token":"$token"""") &&
            json.contains(""""text":"$text"""") &&
            json.contains(""""type":"Text"""") &&
            json.contains(""""action":"onMessage"""")

    fun isAutostartRequest(json: String) =
        json.contains(""""token":"$token"""") &&
            json.contains(""""action":"onMessage"""") &&
            json.contains(""""eventType":"Presence"""") &&
            json.contains(""""type":"Join"""")

    fun isClearConversationRequest(json: String) =
        json.contains(""""token":"$token"""") &&
            json.contains(""""action":"onMessage"""") &&
            json.contains(""""eventType":"Presence"""") &&
            json.contains(""""type":"Clear"""")

    fun isCloseAllConnectionsRequest(json: String) =
        json.contains(""""token":"$token"""") &&
            json.contains(""""closeAllConnections":true""") &&
            json.contains(""""action":"closeSession"""")

    fun isRefreshAttachmentUrlRequest(json: String, attachmentId: String = "88888888-8888-8888-8888-888888888888") =
        json.contains(""""token":"$token"""") &&
            json.contains(""""attachmentId":"$attachmentId"""") &&
            json.contains(""""action":"getAttachment"""")

    fun isAttachmentRequest(json: String, attachmentId: String = "88888888-8888-8888-8888-888888888888") =
        json.contains(""""token":"$token"""") &&
            json.contains(""""attachmentId":"$attachmentId"""") &&
            json.contains(""""action":"onAttachment"""")

    fun isDeleteAttachmentRequest(json: String, attachmentId: String = "88888888-8888-8888-8888-888888888888") =
        json.contains(""""token":"$token"""") &&
            json.contains(""""attachmentId":"$attachmentId"""") &&
            json.contains(""""action":"deleteAttachment"""")

    fun isQuickReplyRequest(json: String) =
        json.contains(""""token":"$token"""") &&
            json.contains(""""action":"onMessage"""") &&
            json.contains(""""contentType":"ButtonResponse"""") &&
            json.contains(""""type":"QuickReply"""")

    fun isStructuredMessageRequest(json: String) =
        json.contains(""""token":"$token"""") &&
            json.contains(""""action":"onMessage"""") &&
            json.contains(""""type":"Structured"""")
}
