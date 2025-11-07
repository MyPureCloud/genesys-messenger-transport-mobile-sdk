package com.genesys.cloud.messenger.transport.core.events

import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.send.UserTypingRequest
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.TracingIds
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages

internal const val TYPING_INDICATOR_COOL_DOWN_MILLISECONDS = 5000L

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
            if (delta > TYPING_INDICATOR_COOL_DOWN_MILLISECONDS) {
                lastSentUserTypingTimestamp = currentTimestamp
                val request = UserTypingRequest(token = token, tracingId = TracingIds.newId())
                WebMessagingJson.json.encodeToString(request)
            } else {
                log.w { LogMessages.typingIndicatorCoolDown(TYPING_INDICATOR_COOL_DOWN_MILLISECONDS) }
                null
            }
        } else {
            log.w { LogMessages.TYPING_INDICATOR_DISABLED }
            null
        }
    }

    fun clear() {
        lastSentUserTypingTimestamp = 0L
    }
}
