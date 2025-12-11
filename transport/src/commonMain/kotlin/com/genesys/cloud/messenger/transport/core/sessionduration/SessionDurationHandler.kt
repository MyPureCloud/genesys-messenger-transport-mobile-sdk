package com.genesys.cloud.messenger.transport.core.sessionduration

import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.EventHandler
import com.genesys.cloud.messenger.transport.util.ActionTimer
import com.genesys.cloud.messenger.transport.util.DEFAULT_HEALTH_CHECK_PRE_NOTICE_TIME_MILLIS
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages

/**
 * Handles session duration tracking and expiration notifications.
 *
 * @param sessionExpirationNoticeInterval The time in seconds when the session expiration notice
 * should be shown before the expiration date.
 * @param healthCheckPreNoticeTimeMillis The time in milliseconds before the expiration notice
 * when a health check should be triggered to verify if the session is still valid.
 * @param eventHandler Handler for emitting events to the messaging client.
 * @param log Logger instance for logging session duration events.
 * @param triggerHealthCheck Callback to trigger a health check request.
 * @param getCurrentTimestamp Function to get the current timestamp in milliseconds.
 */
internal class SessionDurationHandler(
    private val sessionExpirationNoticeInterval: Long,
    private val eventHandler: EventHandler,
    private val log: Log,
    private val getCurrentTimestamp: () -> Long = { Platform().epochMillis() },
    private val healthCheckPreNoticeTimeMillis: Long = DEFAULT_HEALTH_CHECK_PRE_NOTICE_TIME_MILLIS,
    private var triggerHealthCheck: () -> Unit = {},
) {
    private var currentDurationSeconds: Long? = null
    private var currentExpirationDate: Long? = null

    private val expirationTimer: ActionTimer =
        ActionTimer(
            log = log,
            action = { emitSessionExpirationNotice() }
        )

    private val healthCheckTimer: ActionTimer =
        ActionTimer(
            log = log,
            action = { triggerHealthCheck() }
        )

    /**
     * Updates the session duration parameters.
     *
     * @param durationSeconds The time of inactivity (in seconds) before the session expires.
     * @param expirationDate The current expiration date for the session (timestamp).
     */
    fun updateSessionDuration(durationSeconds: Long?, expirationDate: Long?) {
        log.i { LogMessages.updateSessionDuration(durationSeconds, expirationDate) }

        if (durationSeconds != null && durationSeconds != currentDurationSeconds) {
            currentDurationSeconds = durationSeconds
            emitSessionDurationEvent(durationSeconds)
        }

        if (expirationDate != null && expirationDate != currentExpirationDate) {
            currentExpirationDate = expirationDate
            handleExpirationDateChange(expirationDate)
        }
    }

    private fun emitSessionDurationEvent(durationSeconds: Long) {
        eventHandler.onEvent(Event.SessionDuration(durationSeconds))
    }

    private fun handleExpirationDateChange(expirationDate: Long) {
        val noticeTimeSeconds = expirationDate - sessionExpirationNoticeInterval
        val noticeTimeMillis = noticeTimeSeconds * 1000
        val currentTimeMillis = getCurrentTimestamp()
        val expirationNoticeDelayMillis = noticeTimeMillis - currentTimeMillis

        if (expirationNoticeDelayMillis > 0) {
            scheduleHealthCheckTimer(expirationNoticeDelayMillis)
            scheduleExpirationNoticeTimer(expirationNoticeDelayMillis)
        } else {
            log.w { LogMessages.noticeTimeAlreadyPassed(expirationNoticeDelayMillis) }
        }
    }

    private fun scheduleHealthCheckTimer(expirationNoticeDelayMillis: Long) {
        val healthCheckDelayMillis = expirationNoticeDelayMillis - healthCheckPreNoticeTimeMillis
        if (healthCheckDelayMillis > 0) {
            log.i { LogMessages.startingHealthCheckTimer(healthCheckDelayMillis) }
            healthCheckTimer.start(healthCheckDelayMillis)
        } else {
            log.i { LogMessages.healthCheckLeadTimeTooShort(healthCheckDelayMillis) }
            triggerHealthCheck()
        }
    }

    private fun scheduleExpirationNoticeTimer(expirationNoticeDelayMillis: Long) {
        log.i { LogMessages.startingExpirationTimer(expirationNoticeDelayMillis, expirationNoticeDelayMillis / 1000) }
        expirationTimer.start(expirationNoticeDelayMillis)
    }

    private fun emitSessionExpirationNotice() {
        val expiresInSeconds = calculateTimeToExpiration()
        eventHandler.onEvent(Event.SessionExpirationNotice(expiresInSeconds))
    }

    private fun calculateTimeToExpiration(): Long {
        val currentTimeSeconds = getCurrentTimestamp() / 1000
        return currentExpirationDate?.let { it - currentTimeSeconds } ?: sessionExpirationNoticeInterval
    }

    fun setTriggerHealthCheck(triggerHealthCheck: () -> Unit) {
        this.triggerHealthCheck = triggerHealthCheck
    }

    fun clear() {
        currentDurationSeconds = null
        currentExpirationDate = null
        expirationTimer.cancel()
        healthCheckTimer.cancel()
    }
}
