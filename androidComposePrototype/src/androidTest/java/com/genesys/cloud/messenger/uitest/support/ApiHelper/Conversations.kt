package com.genesys.cloud.messenger.uitest.support.ApiHelper

import android.util.Log
import com.genesys.cloud.messenger.androidcomposeprototype.ui.testbed.TestBedViewModel
import com.genesys.cloud.messenger.uitest.support.testConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.lang.Thread.sleep

private val TAG = TestBedViewModel::class.simpleName

@Serializable
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

@Serializable
data class Participant(
    val id: String,
    val purpose: String,
    val userId: String?,
    var messages: Array<CallDetails>
)

@Serializable
data class Media(
    val id: String,
    val uploadUrl: String,
    val Status: String
)

@Serializable
data class CallDetails(
    val state: String,
    val id: String
) {
    fun isConnected(): Boolean = state == "connected"

    fun isDisconnected(): Boolean = state == "disconnected" || state == "terminated"

    fun isAlerting(): Boolean = state == "alerting"
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
    val entities = publicApiCall("GET", "/api/v2/conversations")
        ?.jsonObject?.get("entities")?.jsonArray
    return parseJsonToClass(entities?.toList())
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
    var wrapupCodePayload: JsonElement? = null
    if (!connecting) {
        statusPayload = "{\"state\": \"DISCONNECTED\"}"
        wrapupCodePayload = getDefaultWrapupCodeId(conversationInfo.id, agentParticipant!!.id)
    } else {
        statusPayload = "{\"state\": \"CONNECTED\"}"
    }
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
): JsonElement? {
    val wrapupCodeId = publicApiCall("GET", "/api/v2/conversations/$conversationId/participants/$participantId/wrapupcodes")
        ?.jsonArray?.firstOrNull()?.jsonObject?.get("id")
    return wrapupCodeId
}

fun API.waitForParticipantToConnectOrDisconnect(
    conversationId: String,
    connectionState: String = "disconnected"
) {
    val deadline = System.currentTimeMillis() + 60_000L
    while (System.currentTimeMillis() < deadline) {
        try {
            val listOfMessages = getConversationInfo(conversationId).getParticipantFromPurpose("agent")?.messages?.toList()
            if (listOfMessages?.get(0)?.state == connectionState) return
        } catch (_: Exception) {
            // ignore and retry
        }
        sleep(1000)
    }
    throw AssertionError("Participant did not reach state '$connectionState' within 60 seconds")
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
    wrapupPayload: JsonElement?
) {
    val agentParticipant = conversationInfo.getParticipantFromPurpose("agent")
    val communicationId = conversationInfo.getCommunicationId(agentParticipant!!)
    val codeId = wrapupPayload?.jsonPrimitive?.content
    publicApiCall(
        "POST",
        "/api/v2/conversations/messages/${conversationInfo.id}/participants/${agentParticipant.id}/communications/$communicationId/wrapup",
        ("{ \"code\": \"$codeId\", \"notes\": \"\"}").toByteArray()
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
    val mediaResult = publicApiCall(
        "POST",
        "/api/v2/conversations/messages/${conversationInfo.id}/communications/$communicationId/messages/media"
    )
    val result: Media = parseJsonToClass(mediaResult)
}
