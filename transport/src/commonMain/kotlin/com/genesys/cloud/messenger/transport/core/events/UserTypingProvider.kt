package com.genesys.cloud.messenger.transport.core.events

import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.send.UserTypingRequest
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.logs.Log
import kotlinx.serialization.encodeToString

internal const val TYPING_INDICATOR_COOL_DOWN_IN_MILLISECOND = 5000L

internal class UserTypingProvider(
    private val log: Log,
    private val showUserTypingEnabled: () -> Boolean,
    private val getCurrentTimestamp: () -> Long = { Platform().epochMillis() },
) {
    private var lastSentUserTypingTimestamp = 0L

    @Throws(Exception::class)
    fun encodeRequest(token: String): String? {
        return if (showUserTypingEnabled()) {
            val currentTimestamp = getCurrentTimestamp()
            val delta = currentTimestamp - lastSentUserTypingTimestamp
            if (delta > TYPING_INDICATOR_COOL_DOWN_IN_MILLISECOND) {
                lastSentUserTypingTimestamp = currentTimestamp
                val request = UserTypingRequest(token = token)
                WebMessagingJson.json.encodeToString(request)
            } else {
                log.w { "Typing event can be sent only once every $TYPING_INDICATOR_COOL_DOWN_IN_MILLISECOND milliseconds." }
                null
            }
        } else {
            log.w { "typing indicator is disabled." }
            null
        }
    }

    fun clear() {
        lastSentUserTypingTimestamp = 0L
    }
}
