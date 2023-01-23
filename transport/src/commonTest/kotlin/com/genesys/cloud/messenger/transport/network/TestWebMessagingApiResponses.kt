package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.shyrka.receive.Apps
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
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.Styles

object TestWebMessagingApiResponses {

    internal const val isoTestTimestamp = "2014-04-30T21:09:51.411Z"
    internal const val messageEntityResponseWith2Messages =
        """{"entities":[{"id":"5befde6373a23f32f20b59b4e1cba0e6","channel":{"time":"$isoTestTimestamp"},"type":"Text","text":"\uD83E\uDD2A","content":[],"direction":"Outbound","originatingEntity":"Bot"},{"id":"46e7001c24abed05e9bcd1a006eb54b7","channel":{"time":null},"type":"Text","events":[{"eventType":"Presence","presence":{"type":"Join"}}],"metadata":{"customMessageId":"1234567890"},"text":"customer msg 7","content":[],"direction":"Inbound","originatingEntity":"Human"}],"pageSize":25,"pageNumber":1, "total": 2, "pageCount": 1}"""

    internal const val messageEntityListResponseWithoutMessages =
        """{"entities":[],"pageSize":0,"pageNumber":1, "total": 0, "pageCount": 0}"""

    internal const val deploymentConfigResponse =
        """{"id":"test_config_id","version":"3","languages":["en-us"],"defaultLanguage":"en-us","apiEndpoint":"https://api.inindca.com","messenger":{"enabled":true,"apps":{"conversations":{"messagingEndpoint":"wss://webmessaging.inindca.com","conversationDisconnect":{"enabled":true,"type":"ReadOnly"}}},"styles":{"primaryColor":"#ff0000"},"launcherButton":{"visibility":"On"},"fileUpload":{"modes":[{"fileTypes":["image/png","image/jpeg","image/gif"],"maxFileSizeKB":10000}]}},"journeyEvents":{"enabled":true},"status":"Active"}"""

    internal val testMessageEntityList =
        MessageEntityList(
            entities = buildEntities(),
            pageSize = 25,
            pageNumber = 1,
            total = 2,
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
    internal val testDeploymentConfig: DeploymentConfig = DeploymentConfig(
        id = "test_config_id",
        version = "3",
        languages = listOf("en-us"),
        defaultLanguage = "en-us",
        apiEndpoint = "https://api.inindca.com",
        messenger = Messenger(
            enabled = true,
            apps = Apps(
                conversations = Conversations(
                    messagingEndpoint = "wss://webmessaging.inindca.com",
                    conversationDisconnect = Conversations.ConversationDisconnect(
                        enabled = true,
                        type = Conversations.ConversationDisconnect.Type.ReadOnly
                    )
                )
            ),
            styles = Styles(primaryColor = "#ff0000"),
            launcherButton = LauncherButton(visibility = "On"),
            fileUpload = FileUpload(
                modes = listOf(
                    Mode(
                        fileTypes = listOf("image/png", "image/jpeg", "image/gif"),
                        maxFileSizeKB = 10000
                    )
                )
            )
        ),
        journeyEvents = JourneyEvents(enabled = true),
        status = DeploymentConfig.Status.Active,
    )

    private fun buildEntities(): List<StructuredMessage> =
        listOf(
            messageEntity(
                id = "5befde6373a23f32f20b59b4e1cba0e6",
                time = isoTestTimestamp,
                text = "\uD83E\uDD2A",
                isInbound = false,
                originatingEntity = "Bot",
            ),
            messageEntity(
                id = "46e7001c24abed05e9bcd1a006eb54b7",
                time = null,
                text = "customer msg 7",
                customMessageId = "1234567890",
                events = listOf(
                    PresenceEvent(
                        eventType = StructuredMessageEvent.Type.Presence,
                        presence = PresenceEvent.Presence(PresenceEvent.Presence.Type.Join)
                    )
                ),
                originatingEntity = "Human",
            )
        )

    private fun messageEntity(
        id: String,
        time: String?,
        text: String,
        isInbound: Boolean = true,
        customMessageId: String? = null,
        events: List<StructuredMessageEvent> = emptyList(),
        originatingEntity: String?,
    ): StructuredMessage {
        return StructuredMessage(
            id = id,
            channel = StructuredMessage.Channel(time = time),
            type = StructuredMessage.Type.Text,
            text = text,
            content = emptyList(),
            direction = if (isInbound) "Inbound" else "Outbound",
            metadata = if (customMessageId != null) mapOf("customMessageId" to customMessageId) else emptyMap(),
            events = events,
            originatingEntity = originatingEntity,
        )
    }
}
