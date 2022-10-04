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

fun API.answerNewConversation(): Conversation? {
    changePresence("On Queue")
    val conversation = waitForConversation() ?: return null
    sendConnect(conversation)
    changePresence("Available")
    return getConversationInfo(conversation.id) // Returning the "post connected" conversation info.
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

fun API.sendConnect(conversationInfo: Conversation) {
    val statusPayload = "{\"state\": \"CONNECTED\"}".toByteArray()
    sendConversationStatus(payload = statusPayload, conversationInfo = conversationInfo)
}

fun API.sendDisconnect(conversationInfo: Conversation) {
    val statusPayload = "{\"state\": \"DISCONNECTED\"}".toByteArray()
    val participantInfo = conversationInfo.getParticipantForUserId(agentId)!!
    val wrapupCode = getDefaultWrapupCode(conversationId = conversationInfo.id, participantId = participantInfo.id)[0].id
    sendConversationStatus(payload = statusPayload, conversationInfo = conversationInfo)
    sleep(2000) // Small delay between disconnecting and sending the wrapup code.
    val wrapupPayload = """{ "code": "$wrapupCode", "notes": "" }""".toByteArray()
    publicApiCall(httpMethod = "POST", httpURL = "/api/v2/conversations/messages/${conversationInfo.id}/participants/${participantInfo.id}/communications/${conversationInfo.getCommunicationId(participantInfo)}/wrapup", payload = wrapupPayload)
}

private fun API.sendConversationStatus(payload: ByteArray, conversationInfo: Conversation) {
    publicApiCall(httpMethod = "PATCH", httpURL = "/api/v2/conversations/messages/${conversationInfo.id}/participants/${conversationInfo.getParticipantForUserId(agentId)?.id}", payload = payload)
}

fun API.getDefaultWrapupCode(conversationId: String, participantId: String): Array<WrapupCodes> {
    val result = publicApiCall(httpMethod = "GET", httpURL = "/api/v2/conversations/messages/$conversationId/participants/$participantId/wrapupcodes")
    return parseJsonToClass(result)
}

fun API.sendOutboundSmsMessage(conversationInfo: Conversation, message: String = "Test from an agent.") {
    val participant = conversationInfo.getParticipantForUserId(agentId)!!
    val payload = """{ "textBody": "$message" }""".toByteArray()
    publicApiCall("POST", "/api/v2/conversations/messages/${conversationInfo.id}/communications/${conversationInfo.getCommunicationId(participant)}/messages", payload = payload)
}

fun API.sendTypingIndicator(conversationInfo: Conversation) {
    val participant = conversationInfo.getParticipantForUserId(agentId)!!
    val payload = """{ "textBody": "typing" }""".toByteArray()
    publicApiCall("POST", "/api/v2/conversations/messages/${conversationInfo.id}/communications/${conversationInfo.getCommunicationId(participant)}/typing", payload = payload)
}
