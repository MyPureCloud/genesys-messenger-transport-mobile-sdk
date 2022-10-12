package com.genesys.cloud.messenger.uitest.support.ApiHelper

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.genesys.cloud.messenger.uitest.support.testConfig
import org.awaitility.Awaitility
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit

@JsonIgnoreProperties(ignoreUnknown = true)
data class Conversation(
    val id: String,
    var participants: Array<Participant>
) {
    fun getParticipantForUserId(userId: String): Participant? {
        return participants.firstOrNull { participant ->
            participant.userId == userId && participant.purpose == "agent" && participant.messages.firstOrNull()?.isDisconnected()?.not() ?: false
        }
    }

    fun getParticipantFromPurpose(purpose: String): Participant? {
        return participants.firstOrNull { participant ->
            participant.purpose == purpose && participant.messages.firstOrNull()?.isDisconnected()?.not() ?: false
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

data class WrapUpPayload(val codeId: String, val notes: String)
var statusPayload = "{\"state\": \"CONNECTED\"}"

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
        if (conversations.firstOrNull() != null) {
            return conversations[0]
        }
        sleep(1000)
    }
    return null
}

fun API.getAllConversations(): Array<Conversation> {
    val result = publicApiCall(httpMethod = "GET", httpURL = "/api/v2/conversations")?.get("entities")?.toList()
    return parseJsonToClass(result)
}

fun API.getConversationInfo(conversationId: String): Conversation {
    val result = publicApiCall(httpMethod = "GET", httpURL = "/api/v2/conversations/$conversationId")
    return parseJsonToClass(result)
}

fun API.sendTypingIndicator(conversationInfo: Conversation) {
    val agentParticipant = conversationInfo.getParticipantFromPurpose("agent")
    val participant = conversationInfo.getParticipantForUserId(agentParticipant!!.userId!!)
    val payload = "{\"typing\": {\"type\": \"On\"}}".toByteArray()
    publicApiCall("POST", "/api/v2/conversations/messages/${conversationInfo.id}/communications/${conversationInfo.getCommunicationId(participant!!)}/typing", payload = payload)
}

fun API.answerNewConversation(): Conversation? {
    changePresence("On Queue", testConfig.agentUserId)
    val conversation = waitForConversation()
    if (conversation == null) {
        println("Failed to receive a new conversation to answer.")
        return null
    }
    sendConnectOrDisconnect(conversation, true, false)
    changePresence("Available", testConfig.agentUserId)
    return getConversationInfo(conversation.id)
}

fun API.sendConnectOrDisconnect(conversationInfo: Conversation, connecting: Boolean, wrapup: Boolean = true) {
    println("Sending $connecting request to: ${conversationInfo.id} from a conversation.")
    val agentParticipant = conversationInfo.getParticipantFromPurpose("agent")
    var wrapupCodePayload = WrapUpPayload("", "")
    if (!connecting) {
        statusPayload = "{\"state\": \"DISCONNECTED\"}"
        val wrapupCodeId = getDefaultWrapupCode(conversationInfo.id, agentParticipant!!.id!!)
        wrapupCodePayload = WrapUpPayload(wrapupCodeId, "")
    }
    // Send requests to disconnect the conversation and send the wrapup code.
    sendStatusToParticipant(conversationInfo.id, agentParticipant!!.id)
    if (connecting) {
        waitForParticipantToConnect(conversationInfo.id)
    } else if (wrapup) {
        sleep(2000)
        print("Sending wrapup code.")
        sendWrapupCode(conversationInfo, wrapupCodePayload)
    }
}

fun API.getDefaultWrapupCode(conversationId: String, participantId: String): String {
    return publicApiCall("GET", "/api/v2/conversations/$conversationId/participants/$participantId/wrapupcodes")?.firstOrNull()?.get("id").toString()
}

private fun API.waitForParticipantToConnect(conversationId: String) {
    Awaitility.await().atMost(60, TimeUnit.SECONDS).ignoreExceptions()
        .untilAsserted {
            val listOfMessages = getConversationInfo(conversationId).getParticipantFromPurpose("agent")?.messages?.toList()
            listOfMessages?.get(0)?.state == "connected"
        }
}

fun API.sendStatusToParticipant(conversationId: String, participantId: String) {
    publicApiCall("PATCH", "/api/v2/conversations/messages/$conversationId/participants/$participantId", statusPayload.toByteArray())
}

fun API.sendWrapupCode(conversationInfo: Conversation, wrapupPayload: WrapUpPayload) {
    val agentParticipant = conversationInfo.getParticipantFromPurpose("agent")
    val communicationId = conversationInfo.getCommunicationId(agentParticipant!!)
    publicApiCall(
        "POST",
        "/api/v2/conversations/messages/${conversationInfo.id}/participants/${agentParticipant?.id}/communications/" +
            "$communicationId/wrapup",
        wrapupPayload.codeId.toByteArray()
    )
}
