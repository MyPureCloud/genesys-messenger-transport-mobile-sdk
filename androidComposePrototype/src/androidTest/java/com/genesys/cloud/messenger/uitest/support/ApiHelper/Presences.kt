package com.genesys.cloud.messenger.uitest.support.ApiHelper

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class AllPresences(
    val entities: Array<PresenceInfo>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PresenceInfo(
    val id: String,
    val systemPresence: String,
    val deactivated: Boolean,
    val primary: Boolean
)

private fun API.getAllPresences(): AllPresences {
    val result = publicApiCall(httpMethod = "GET", httpURL = "/api/v2/presencedefinitions?pageSize=100")
    return parseJsonToClass(result)
}

private var savedPresenceList: MutableMap<String, String>? = null

fun API.getPresenceList(): MutableMap<String, String> {
    if (savedPresenceList != null) {
        val presenceList = savedPresenceList!!
        return presenceList
    }
    val allPresences = getAllPresences()
    val presenceList: MutableMap<String, String> = mutableMapOf()
    for (presenceInfo in allPresences.entities) {
        if (presenceInfo.primary) {
            presenceList.put(presenceInfo.systemPresence, presenceInfo.id)
        }
    }
    savedPresenceList = presenceList
    return presenceList
}

fun API.changePresence(presenceName: String, agentId: String, message: String = "") {
    val presenceList = getPresenceList()
    val presenceID = presenceList[presenceName]
    val payload = "{\"primary\": \"true\",\"presenceDefinition\": {\"id\": \"$presenceID\"},\"message\": \"$message\" }".toByteArray()
    publicApiCall("PATCH", "/api/v2/users/$agentId/presences/PURECLOUD", payload)
}
