package com.genesys.cloud.messenger.transport.util

import com.genesys.cloud.messenger.transport.core.Message
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object Response {
    fun configureSuccess(
        connected: Boolean = true,
        readOnly: Boolean = false,
        allowedMedia: String = AllowedMedia.empty,
        blockedExtensions: String = AllowedMedia.emptyBlockedExtensions,
    ): String =
        """{"type":"response","class":"SessionResponse","code":200,"body":{"connected":$connected,"newSession":true,"readOnly":$readOnly$allowedMedia$blockedExtensions}}"""
    const val configureSuccessWithNewSessionFalse =
        """{"type":"response","class":"SessionResponse","code":200,"body":{"connected":true,"newSession":false}}"""
    const val webSocketRequestFailed =
        """{"type":"response","class":"string","code":400,"body":"Request failed."}"""
    const val defaultStructuredEvents =
        """{"eventType": "Typing","typing": {"type": "Off","duration": 1000}},{"eventType": "Typing","typing": {"type": "On","duration": 5000}}"""
    const val onMessage =
        """{"type":"message","class":"StructuredMessage","code":200,"body":{"text":"Hi!","direction":"Inbound","id":"test_id","channel":{"time":"2022-08-22T19:24:26.704Z","messageId":"message_id"},"type":"Text","metadata":{"customMessageId":"some_custom_message_id"}}}"""
    const val onMessageWithAttachment =
        """{"type":"message","class":"StructuredMessage","code":200,"body":{"direction":"Outbound","id":"msg_id","channel":{"time":"some_time","type":"Private"},"type":"Text","text":"Hi","content":[{"attachment":{"id":"attachment_id","filename":"image.png","mediaType":"Image","mime":"image/png","url":"https://downloadurl.com"},"contentType":"Attachment"}],"originatingEntity":"Human"}}"""
    const val attachmentDeleted =
        """{"type":"message","class":"AttachmentDeletedResponse","code":200,"body":{"attachmentId":"attachment_id"}}"""
    const val typingIndicatorForbidden =
        """{"type":"response","class":"string","code":403,"body":"Turn on the Feature Toggle or fix the configuration."}"""
    const val sessionNotFound =
        """{"type": "response","class": "string","code": 4007,"body": "session not found error message"}"""
    const val sessionExpired =
        """{"type": "response","class": "string","code": 4006,"body": "session expired error message"}"""
    const val sessionExpiredEvent =
        """{"type": "response","class": "SessionExpiredEvent","code": 200,"body": {}}"""
    const val messageTooLong =
        """{"type": "response","class": "string","code": 4011,"body": "message too long"}"""
    const val tooManyRequests =
        """{"type":"response","class":"TooManyRequestsErrorMessage","code":429,"body":{"retryAfter":3,"errorCode":4029,"errorMessage":"Message rate too high for this session"}}"""
    const val customAttributeSizeTooLarge =
        """{"type": "response","class": "string","code": 4013,"body": "Custom Attributes in channel metadata is larger than 2048 bytes"}"""
    const val connectionClosedEvent =
        """{"type":"message","class":"ConnectionClosedEvent","code":200,"body":{}}"""
    const val logoutEvent =
        """{"type":"message","class":"LogoutEvent","code":200,"body":{}}"""
    const val unauthorized =
        """{"type": "response","class": "string","code": 401,"body": "User is unauthorized"}"""
    const val sessionClearedEvent =
        """{"type":"message","class":"SessionClearedEvent","code":200,"body":{}}"""
    fun clearConversationForbidden(errorMessage: String = "Presence events Conversation Clear are not supported") =
        """{"type":"response","class":"string","code":403,"body":"$errorMessage"}"""

    fun structuredMessageWithEvents(
        events: String = defaultStructuredEvents,
        direction: Message.Direction = Message.Direction.Outbound,
        metadata: Map<String, String> = mapOf("correlationId" to "0000000-0000-0000-0000-0000000000"),
    ): String {
        return """{"type": "message","class": "StructuredMessage","code": 200,"body": {"direction": "${direction.name}","id": "0000000-0000-0000-0000-0000000000","channel": {"time": "2022-03-09T13:35:31.104Z","messageId": "0000000-0000-0000-0000-0000000000"},"type": "Event","metadata": ${
        Json.encodeToString(metadata)
        },"events": [$events]}}"""
    }

    fun presignedUrlResponse(
        attachmentId: String = "88888888-8888-8888-8888-888888888888",
        headers: String = "",
        url: String = "https://downloadUrl.com",
        fileSize: Int = 1,
        fileName: String = "test_asset.png",
        fileType: String = "image/jpeg",
    ): String {
        return """{"type":"response","class":"PresignedUrlResponse","code":200,"body":{"attachmentId":"$attachmentId","headers":{$headers},"url":"$url","fileName":"$fileName","fileSize":$fileSize,"fileType":"$fileType"}}"""
    }

    internal object AllowedMedia {
        const val empty = ""
        const val emptyBlockedExtensions = ""
        const val listOfBlockedExtensions = ""","blockedExtensions":[".ade",".adp"]"""
        const val noInbound = ""","allowedMedia":{}"""
        const val emptyInbound = ""","allowedMedia":{"inbound":{}}"""
        const val listOfFileTypesWithMaxSize = ""","allowedMedia":{"inbound":{"fileTypes":[{"type":"video/mpg"},{"type":"video/3gpp"}],"maxFileSizeKB":10240}}"""
        const val listOfFileTypesWithWildcardAndMaxSize = ""","allowedMedia":{"inbound":{"fileTypes":[{"type":"*/*"},{"type":"video/3gpp"}],"maxFileSizeKB":10240}}"""
    }
}
