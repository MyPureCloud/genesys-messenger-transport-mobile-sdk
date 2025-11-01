package com.genesys.cloud.messenger.uitest.support.ApiHelper

import android.util.Log
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import com.genesys.cloud.messenger.androidcomposeprototype.ui.testbed.TestBedViewModel
import com.genesys.cloud.messenger.uitest.support.testConfig
import org.awaitility.Awaitility
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit

private val TAG = TestBedViewModel::class.simpleName

@JsonIgnoreProperties(ignoreUnknown = true)
data class Conversation(
    val id: String,
    var participants: Array<Participant>
) {
    fun getParticipantForUserId(userId: String): Participant? {
        return participants.firstOrNull { participant ->
            participant.userId == userId &&
                participant.purpose == "agent" &&
                participant.messages
                    .firstOrNull()
                    ?.isDisconnected()
                    ?.not() ?: false
        }
    }

    fun getParticipantFromPurpose(purpose: String): Participant? {
        return participants.firstOrNull { participant ->
            participant.purpose == purpose &&
                participant.messages
                    .firstOrNull()
                    ?.isDisconnected()
                    ?.not() ?: false
        }
    }

    fun getCommunicationId(participant: Participant): String {
        return participant.messages[0].id
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Participant(
    val id: String,
    val purpose: String,
    val userId: String?,
    var messages: Array<CallDetails>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Media(
    val id: String,
    val uploadUrl: String,
    val Status: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CallDetails(
    val state: String,
    val id: String
) {
    fun isConnected(): Boolean {
        return state == "connected"
    }

    fun isDisconnected(): Boolean {
        return state == "disconnected" || state == "terminated"
    }

    fun isAlerting(): Boolean {
        return state == "alerting"
    }
}

fun API.waitForConversation(): Conversation? {
    for (x in 0..60) {
        val conversations = getAllConversations()
        if (conversations != null) {
            conversations.forEach { conversation ->
                Log.i(TAG, "conversationId: $conversation.id")
                val callDetails = conversation.getParticipantFromPurpose("agent")?.messages
                if (callDetails != null) {
                    callDetails.forEach { callDetail ->
                        Log.i(TAG, "call detail state: $callDetail.state")
                        if (callDetail.isAlerting()) {
                            return conversation
                        }
                    }
                }
            }
        }
        sleep(1000)
    }
    return null
}

fun API.getAllConversations(): Array<Conversation> {
    val result = publicApiCall("GET", "/api/v2/conversations")?.get("entities")?.toList()
    return parseJsonToClass(result)
}

fun API.getConversationInfo(conversationId: String): Conversation {
    val result = publicApiCall("GET", "/api/v2/conversations/$conversationId")
    return parseJsonToClass(result)
}

fun API.sendTypingIndicatorFromAgentToUser(conversationInfo: Conversation) {
    val agentParticipant = conversationInfo.getParticipantFromPurpose("agent")
    val participant = conversationInfo.getParticipantForUserId(agentParticipant!!.userId!!)
    val payload = "{\"typing\": {\"type\": \"On\"}}".toByteArray()
    publicApiCall("POST", "/api/v2/conversations/messages/${conversationInfo.id}/communications/${conversationInfo.getCommunicationId(participant!!)}/typing", payload = payload)
}

fun API.sendOutboundMessageFromAgentToUser(
    conversationInfo: Conversation,
    message: String
) {
    val agentParticipant = conversationInfo.getParticipantFromPurpose("agent")
    val communicationId = conversationInfo.getCommunicationId(agentParticipant!!)
    val payload = "{\"textBody\": \"${message}\"}".toByteArray()
    publicApiCall("POST", "/api/v2/conversations/messages/${conversationInfo.id}/communications/$communicationId/messages", payload)
}

fun API.answerNewConversation(): Conversation? {
    changePresence("On Queue", testConfig.agentUserId)
    val conversation = waitForConversation()
    if (conversation == null) {
        println("Failed to receive a new conversation to answer.")
        return null
    }
    sendConnectOrDisconnect(conversation, connecting = true, wrapup = false)
    changePresence("Available", testConfig.agentUserId)
    return getConversationInfo(conversation.id)
}

fun API.sendConnectOrDisconnect(
    conversationInfo: Conversation,
    connecting: Boolean = false,
    wrapup: Boolean = true
) {
    var statusPayload = ""
    println("Sending $connecting request to: ${conversationInfo.id} from a conversation.")
    val agentParticipant = conversationInfo.getParticipantFromPurpose("agent")
    var wrapupCodePayload: JsonNode? = null
    if (!connecting) {
        statusPayload = "{\"state\": \"DISCONNECTED\"}"
        wrapupCodePayload = getDefaultWrapupCodeId(conversationInfo.id, agentParticipant!!.id)
    } else {
        statusPayload = "{\"state\": \"CONNECTED\"}"
    }
    // Send requests to connect/disconnect the conversation and send the wrapup code.
    sendStatusToParticipant(conversationInfo.id, agentParticipant!!.id, statusPayload)
    if (connecting) {
        waitForParticipantToConnectOrDisconnect(conversationInfo.id, connectionState = "connected")
    } else if (wrapup) {
        sleep(2000)
        print("Sending wrapup code.")
        sendWrapupCode(conversationInfo, wrapupCodePayload)
    }
}

fun API.getDefaultWrapupCodeId(
    conversationId: String,
    participantId: String
): JsonNode? {
    val wrapupCodeId = publicApiCall("GET", "/api/v2/conversations/$conversationId/participants/$participantId/wrapupcodes")?.firstOrNull()?.get("id")
    return wrapupCodeId
}

fun API.waitForParticipantToConnectOrDisconnect(
    conversationId: String,
    connectionState: String = "disconnected"
) {
    Awaitility
        .await()
        .atMost(60, TimeUnit.SECONDS)
        .ignoreExceptions()
        .untilAsserted {
            val listOfMessages = getConversationInfo(conversationId).getParticipantFromPurpose("agent")?.messages?.toList()
            listOfMessages?.get(0)?.state == connectionState
        }
}

fun API.sendStatusToParticipant(
    conversationId: String,
    participantId: String,
    statusPayload: String
) {
    publicApiCall("PATCH", "/api/v2/conversations/messages/$conversationId/participants/$participantId", statusPayload.toByteArray())
}

fun API.sendWrapupCode(
    conversationInfo: Conversation,
    wrapupPayload: JsonNode?
) {
    val agentParticipant = conversationInfo.getParticipantFromPurpose("agent")
    val communicationId = conversationInfo.getCommunicationId(agentParticipant!!)
    publicApiCall(
        "POST",
        "/api/v2/conversations/messages/${conversationInfo.id}/participants/${agentParticipant?.id}/communications/$communicationId/wrapup",
        ("{ \"code\": \"${wrapupPayload?.get("id")}\", \"notes\": \"\"}").toByteArray()
    )
}

fun API.disconnectAllConversations() {
    val conversationList = getAllConversations()
    conversationList.forEach { conversation ->
        Log.i(TAG, "disconnecting conversationId: ${conversation.id}")
        sendConnectOrDisconnect(conversation)
    }
}

fun API.checkForConversationMessages(conversationId: String) {
    val listOfMessages = getConversationInfo(conversationId).getParticipantFromPurpose("agent")?.messages?.toList()
    if (listOfMessages != null) AssertionError("Conversation still has messages associated with it but should not")
}

fun API.attachImage(conversationInfo: Conversation) {
    val agentParticipant = conversationInfo.getParticipantFromPurpose("agent")
    val communicationId = conversationInfo.getCommunicationId(agentParticipant!!)
    val mediaResult =
        publicApiCall(
            "POST",
            "/api/v2/conversations/messages/${conversationInfo.id}/communications/$communicationId/messages/media"
        )
    val result: Media = parseJsonToClass(mediaResult)
}
