package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.shyrka.receive.Apps
import com.genesys.cloud.messenger.transport.shyrka.receive.Auth
import com.genesys.cloud.messenger.transport.shyrka.receive.Conversations
import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig
import com.genesys.cloud.messenger.transport.shyrka.receive.FileUpload
import com.genesys.cloud.messenger.transport.shyrka.receive.JourneyEvents
import com.genesys.cloud.messenger.transport.shyrka.receive.LauncherButton
import com.genesys.cloud.messenger.transport.shyrka.receive.MessageEntityList
import com.genesys.cloud.messenger.transport.shyrka.receive.Messenger
import com.genesys.cloud.messenger.transport.shyrka.receive.Mode
import com.genesys.cloud.messenger.transport.shyrka.receive.PresenceEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage.Content.QuickReplyContent.QuickReply
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.Styles

object TestWebMessagingApiResponses {
    internal const val isoTestTimestamp = "2014-04-30T21:09:51.411Z"
    internal const val messageEntityResponseWith3Messages =
        """{"entities":[{"id":"5befde6373a23f32f20b59b4e1cba0e6","channel":{"time":"$isoTestTimestamp"},"type":"Text","text":"\uD83E\uDD2A","content":[],"direction":"Outbound","originatingEntity":"Bot"},{"id":"46e7001c24abed05e9bcd1a006eb54b7","channel":{"time":null},"type":"Event","events":[{"eventType":"Presence","presence":{"type":"Join"}}],"text":"customer msg 7","content":[],"direction":"Inbound","originatingEntity":"Human"},{"text":"quick reply text","type":"Structured","direction":"Outbound","id":"message3_id","channel":{"time":null},"content":[{"contentType":"QuickReply","quickReply":{"text":"text_a","payload":"payload_a","action":"action_a"}},{"contentType":"QuickReply","quickReply":{"text":"text_b","payload":"payload_b","action":"action_b"}}],"originatingEntity":"Bot"}],"pageSize":25,"pageNumber":1, "total": 3, "pageCount": 1}"""

    internal const val messageEntityListResponseWithoutMessages =
        """{"entities":[],"pageSize":0,"pageNumber":1, "total": 0, "pageCount": 0}"""

    internal const val deploymentConfigResponse =
        """{"id":"test_config_id","version":"3","languages":["en-us"],"defaultLanguage":"en-us","apiEndpoint":"https://api.inindca.com","messenger":{"enabled":true,"apps":{"conversations":{"messagingEndpoint":"wss://webmessaging.inindca.com","conversationDisconnect":{"enabled":true,"type":"ReadOnly"},"conversationClear":{"enabled":true}}},"styles":{"primaryColor":"#ff0000"},"launcherButton":{"visibility":"On"},"fileUpload":{"modes":[{"fileTypes":["image/png","image/jpeg","image/gif"],"maxFileSizeKB":10000}]}},"journeyEvents":{"enabled":true},"status":"Active","auth":{"enabled":true,"allowSessionUpgrade":true}}"""

    internal val testMessageEntityList =
        MessageEntityList(
            entities = buildEntities(),
            pageSize = 25,
            pageNumber = 1,
            total = 3,
            pageCount = 1
        )
    internal val emptyMessageEntityList =
        MessageEntityList(
            entities = emptyList(),
            pageSize = 0,
            pageNumber = 1,
            total = 0,
            pageCount = 0
        )
    internal val testDeploymentConfig: DeploymentConfig =
        DeploymentConfig(
            id = "test_config_id",
            version = "3",
            languages = listOf("en-us"),
            defaultLanguage = "en-us",
            apiEndpoint = "https://api.inindca.com",
            messenger =
                Messenger(
                    enabled = true,
                    apps =
                        Apps(
                            conversations =
                                Conversations(
                                    messagingEndpoint = "wss://webmessaging.inindca.com",
                                    conversationDisconnect =
                                        Conversations.ConversationDisconnect(
                                            enabled = true,
                                            type = Conversations.ConversationDisconnect.Type.ReadOnly
                                        ),
                                    conversationClear =
                                        Conversations.ConversationClear(
                                            enabled = true
                                        ),
                                    markdown =
                                        Conversations.Markdown(
                                            enabled = false
                                        )
                                )
                        ),
                    styles = Styles(primaryColor = "#ff0000"),
                    launcherButton = LauncherButton(visibility = "On"),
                    fileUpload =
                        FileUpload(
                            modes =
                                listOf(
                                    Mode(
                                        fileTypes = listOf("image/png", "image/jpeg", "image/gif"),
                                        maxFileSizeKB = 10000
                                    )
                                )
                        )
                ),
            journeyEvents = JourneyEvents(enabled = true),
            status = DeploymentConfig.Status.Active,
            auth =
                Auth(
                    enabled = true,
                    allowSessionUpgrade = true,
                ),
        )

    private fun buildEntities(): List<StructuredMessage> =
        listOf(
            // Text message
            messageEntity(
                id = "5befde6373a23f32f20b59b4e1cba0e6",
                time = isoTestTimestamp,
                text = "\uD83E\uDD2A",
                isInbound = false,
                originatingEntity = "Bot",
            ),
            // Event message
            messageEntity(
                id = "46e7001c24abed05e9bcd1a006eb54b7",
                time = null,
                type = StructuredMessage.Type.Event,
                text = "customer msg 7",
                events =
                    listOf(
                        PresenceEvent(
                            eventType = StructuredMessageEvent.Type.Presence,
                            presence = PresenceEvent.Presence(PresenceEvent.Presence.Type.Join)
                        )
                    ),
                originatingEntity = "Human",
            ),
            // Structured message with Quick Replies
            messageEntity(
                id = "message3_id",
                time = null,
                type = StructuredMessage.Type.Structured,
                text = "quick reply text",
                isInbound = false,
                content =
                    listOf(
                        StructuredMessage.Content.QuickReplyContent(
                            contentType = "QuickReply",
                            QuickReply(
                                text = "text_a",
                                payload = "payload_a",
                                action = "action_a"
                            )
                        ),
                        StructuredMessage.Content.QuickReplyContent(
                            contentType = "QuickReply",
                            QuickReply(
                                text = "text_b",
                                payload = "payload_b",
                                action = "action_b"
                            )
                        ),
                    ),
                originatingEntity = "Bot",
            )
        )

    private fun messageEntity(
        id: String,
        time: String?,
        type: StructuredMessage.Type = StructuredMessage.Type.Text,
        text: String,
        isInbound: Boolean = true,
        events: List<StructuredMessageEvent> = emptyList(),
        content: List<StructuredMessage.Content> = emptyList(),
        originatingEntity: String?,
    ): StructuredMessage {
        return StructuredMessage(
            id = id,
            channel = StructuredMessage.Channel(time = time),
            type = type,
            text = text,
            content = content,
            direction = if (isInbound) "Inbound" else "Outbound",
            events = events,
            originatingEntity = originatingEntity,
        )
    }
}
