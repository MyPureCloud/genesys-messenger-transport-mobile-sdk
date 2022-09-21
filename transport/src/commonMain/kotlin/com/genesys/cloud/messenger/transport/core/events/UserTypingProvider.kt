package com.genesys.cloud.messenger.transport.core.events

import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.send.UserTypingRequest
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.logs.Log
import kotlinx.serialization.encodeToString

private const val TYPING_INDICATOR_COOL_DOWN_IN_MILLISECOND = 5000L

internal class UserTypingProvider(val log: Log, val getCurrentTimestamp: () -> Long = { Platform().epochMillis() }) {
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
            log.w { "Typing event can be sent only once every $TYPING_INDICATOR_COOL_DOWN_IN_MILLISECOND milliseconds." }
            null
        }
    }

    fun clear() {
        lastSentUserTypingTimestamp = 0L
    }
}
