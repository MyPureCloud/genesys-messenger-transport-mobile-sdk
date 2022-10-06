package com.genesys.cloud.messenger.uitest.support.ApiHelper

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

import java.lang.Thread.sleep

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

    fun getParticipantFromPurpose(purpose: String) : Participant? {
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

// At the moment, just need to know the wrapup code id.
@JsonIgnoreProperties(ignoreUnknown = true)
data class WrapupCodes(
    val id: String
)

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
