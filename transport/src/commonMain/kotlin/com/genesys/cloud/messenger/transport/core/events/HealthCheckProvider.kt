package com.genesys.cloud.messenger.transport.core.events

import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.send.EchoRequest
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.logs.Log
import kotlinx.serialization.encodeToString

private const val HEALTH_CHECK_COOL_DOWN_IN_MILLISECOND = 30000L

internal class HealthCheckProvider(val log: Log, val getCurrentTimestamp: () -> Long = { Platform().epochMillis() }) {
    private var lastSentHealthCheckTimestamp = 0L

    @Throws(Exception::class)
    fun encodeRequest(token: String): String? {
        val currentTimestamp = getCurrentTimestamp()
        val delta = currentTimestamp - lastSentHealthCheckTimestamp
        return if (delta > HEALTH_CHECK_COOL_DOWN_IN_MILLISECOND) {
            lastSentHealthCheckTimestamp = currentTimestamp
            val request = EchoRequest(token = token)
            WebMessagingJson.json.encodeToString(request)
        } else {
            log.w { "Health check can be sent only once every $HEALTH_CHECK_COOL_DOWN_IN_MILLISECOND milliseconds." }
            null
        }
    }

    fun clear() {
        lastSentHealthCheckTimestamp = 0L
    }
}