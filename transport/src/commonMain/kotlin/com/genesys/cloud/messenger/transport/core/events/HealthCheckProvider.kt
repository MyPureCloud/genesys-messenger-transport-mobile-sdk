package com.genesys.cloud.messenger.transport.core.events

import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.send.EchoRequest
import com.genesys.cloud.messenger.transport.util.ActionTimer
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.util.logs.LogTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal const val HEALTH_CHECK_COOL_DOWN_MILLISECONDS = 30000L

internal class HealthCheckProvider(
    internal val log: Log = Log(enableLogs = false, LogTag.HEALTH_CHECK_PROVIDER),
    internal val getCurrentTimestamp: () -> Long = { Platform().epochMillis() },
    private var triggerHealthCheck: () -> Unit = {},
    dispatcher: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) {
    private var lastSentHealthCheckTimestamp = 0L

    private val deferredHealthCheckTimer: ActionTimer =
        ActionTimer(
            log = log,
            action = { onDeferredHealthCheckTimerFired() },
            dispatcher = dispatcher
        )

    fun setTriggerHealthCheck(triggerHealthCheck: () -> Unit) {
        this.triggerHealthCheck = triggerHealthCheck
    }

    @Throws(Exception::class)
    fun encodeRequest(token: String): String? {
        val currentTimestamp = getCurrentTimestamp()
        val delta = currentTimestamp - lastSentHealthCheckTimestamp
        return if (delta > HEALTH_CHECK_COOL_DOWN_MILLISECONDS) {
            lastSentHealthCheckTimestamp = currentTimestamp
            val request = EchoRequest(token = token)
            WebMessagingJson.json.encodeToString(request)
        } else {
            val remainingCooldown = HEALTH_CHECK_COOL_DOWN_MILLISECONDS - delta
            log.i { LogMessages.healthCheckDeferred(remainingCooldown) }
            deferredHealthCheckTimer.start(remainingCooldown)
            null
        }
    }

    private fun onDeferredHealthCheckTimerFired() {
        triggerHealthCheck()
    }

    fun clear() {
        lastSentHealthCheckTimestamp = 0L
        deferredHealthCheckTimer.cancel()
    }
}
