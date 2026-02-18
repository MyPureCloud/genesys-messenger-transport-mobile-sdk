package com.genesys.cloud.messenger.transport.core.sessionduration

import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.EventHandler
import com.genesys.cloud.messenger.transport.util.ActionTimer
import com.genesys.cloud.messenger.transport.util.DEFAULT_HEALTH_CHECK_PRE_NOTICE_TIME_MILLIS
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Handles session duration tracking and expiration notifications.
 *
 * @param sessionExpirationNoticeIntervalSeconds How many seconds before the session expires to show
 * the expiration notice.
 * @param healthCheckPreNoticeTimeMillis The time in milliseconds before the expiration notice
 * when a health check should be triggered to verify if the session is still valid.
 * @param eventHandler Handler for emitting events to the messaging client.
 * @param log Logger instance for logging session duration events.
 * @param triggerHealthCheck Callback to trigger a health check request.
 * @param getCurrentTimestamp Function to get the current timestamp in milliseconds.
 * @param dispatcher The coroutine scope to use for timer execution.
 */
internal class SessionDurationHandler(
    private val sessionExpirationNoticeIntervalSeconds: Long,
    private val eventHandler: EventHandler,
    private val log: Log,
    private val getCurrentTimestamp: () -> Long = { Platform().epochMillis() },
    private val healthCheckPreNoticeTimeMillis: Long = DEFAULT_HEALTH_CHECK_PRE_NOTICE_TIME_MILLIS,
    private var triggerHealthCheck: () -> Unit = {},
    dispatcher: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) {
    private var currentDurationSeconds: Long? = null
    private var currentExpirationDate: Long? = null
    private var updatedExpirationDate: Long? = null
    private var sessionExpirationNoticeSent: Boolean = false

    private val expirationTimer: ActionTimer =
        ActionTimer(
            log = log,
            action = { onExpirationTimerFired() },
            dispatcher = dispatcher
        )
    private val healthCheckTimer: ActionTimer =
        ActionTimer(
            log = log,
            action = { onHealthCheckTimerFired() },
            dispatcher = dispatcher
        )
    private val expirationHealthCheckTimer: ActionTimer =
        ActionTimer(
            log = log,
            action = { triggerHealthCheck() },
            dispatcher = dispatcher
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
        val noticeTimeSeconds = expirationDate - sessionExpirationNoticeIntervalSeconds
        val noticeTimeMillis = noticeTimeSeconds * 1000
        val currentTimeMillis = getCurrentTimestamp()
        val expirationNoticeDelayMillis = noticeTimeMillis - currentTimeMillis

        if (expirationNoticeDelayMillis > 0) {
            scheduleHealthCheckTimer(expirationNoticeDelayMillis)
            scheduleExpirationNoticeTimer(expirationNoticeDelayMillis)
        } else {
            log.w { LogMessages.noticeTimeAlreadyPassed(expirationNoticeDelayMillis) }
            emitSessionExpirationNotice()
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

    private fun shouldReschedule(): Boolean {
        val updated = updatedExpirationDate ?: return false
        val current = currentExpirationDate ?: return false
        return updated > current
    }

    private fun onHealthCheckTimerFired() {
        if (shouldReschedule()) {
            updateSessionDuration(null, updatedExpirationDate)
        } else {
            triggerHealthCheck()
        }
    }

    private fun onExpirationTimerFired() {
        if (shouldReschedule()) {
            updateSessionDuration(null, updatedExpirationDate)
        } else {
            emitSessionExpirationNotice()
        }
    }

    private fun emitSessionExpirationNotice() {
        val expiresInSeconds = calculateTimeToExpiration()
        sessionExpirationNoticeSent = true
        log.i { LogMessages.sessionExpirationNoticeSent(expiresInSeconds) }
        eventHandler.onEvent(Event.SessionExpirationNotice(expiresInSeconds))
        scheduleExpirationHealthCheck(expiresInSeconds)
    }

    private fun scheduleExpirationHealthCheck(expiresInSeconds: Long) {
        val delayMillis = expiresInSeconds * 1000
        if (delayMillis > 0) {
            log.i { LogMessages.schedulingExpirationHealthCheck(delayMillis) }
            expirationHealthCheckTimer.start(delayMillis)
        } else {
            triggerHealthCheck()
        }
    }

    private fun calculateTimeToExpiration(): Long {
        val currentTimeSeconds = getCurrentTimestamp() / 1000
        return currentExpirationDate?.let { it - currentTimeSeconds } ?: sessionExpirationNoticeIntervalSeconds
    }

    fun setTriggerHealthCheck(triggerHealthCheck: () -> Unit) {
        this.triggerHealthCheck = triggerHealthCheck
    }

    /**
     * Called when a message is sent or received.
     * Calculates and stores the updated expiration date based on current activity.
     * If a session expiration notice was previously sent, this will emit a RemoveSessionExpirationNotice event
     * and trigger a health check to get the latest session duration values.
     */
    fun onMessage() {
        currentDurationSeconds?.let { duration ->
            updatedExpirationDate = (getCurrentTimestamp() / 1000) + duration
        }

        if (sessionExpirationNoticeSent) {
            log.i { LogMessages.REMOVING_SESSION_EXPIRATION_NOTICE }
            sessionExpirationNoticeSent = false
            expirationHealthCheckTimer.cancel()
            eventHandler.onEvent(Event.RemoveSessionExpirationNotice)
            currentExpirationDate = updatedExpirationDate
            updateSessionDuration(null, updatedExpirationDate)
            triggerHealthCheck()
        }
    }

    /**
     * Clears the session duration handler and emits a RemoveSessionExpirationNotice event
     * if a session expiration notice was previously sent.
     * This should be called when the connection is closed or an error occurs to ensure
     * no stale expiration notices are displayed.
     */
    fun clearAndRemoveNotice() {
        if (sessionExpirationNoticeSent) {
            log.i { LogMessages.CLEARING_SESSION_DURATION_WITH_ACTIVE_NOTICE }
            eventHandler.onEvent(Event.RemoveSessionExpirationNotice)
        }
        clear()
    }

    fun clear() {
        currentDurationSeconds = null
        currentExpirationDate = null
        updatedExpirationDate = null
        sessionExpirationNoticeSent = false
        expirationTimer.cancel()
        healthCheckTimer.cancel()
        expirationHealthCheckTimer.cancel()
    }
}
