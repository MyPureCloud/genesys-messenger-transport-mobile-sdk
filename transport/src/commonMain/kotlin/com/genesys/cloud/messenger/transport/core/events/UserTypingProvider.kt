package com.genesys.cloud.messenger.transport.core.events

import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.send.UserTypingRequest
import com.genesys.cloud.messenger.transport.util.Platform
import kotlinx.serialization.encodeToString

const val TYPING_INDICATOR_COOL_DOWN_IN_MILLISECOND = 5000L

internal class UserTypingProvider(val getCurrentTimestamp: () -> Long = { Platform().epochMillis() }) {
    private var lastSentUserTypingTimestamp = 0L

    @Throws(Exception::class)
    fun encodeRequest(token: String): String? {
        val currentTimestamp = getCurrentTimestamp()
        val delta = currentTimestamp - lastSentUserTypingTimestamp
        return if (delta > TYPING_INDICATOR_COOL_DOWN_IN_MILLISECOND) {
            lastSentUserTypingTimestamp = currentTimestamp
            val request = UserTypingRequest(token = token)
            WebMessagingJson.json.encodeToString(request)
        } else {
            null
        }
    }

    fun clear() {
        lastSentUserTypingTimestamp = 0L
    }
}
