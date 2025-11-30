package com.genesys.cloud.messenger.transport.core.sessionduration

import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.EventHandler
import com.genesys.cloud.messenger.transport.util.ActionTimer
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.logs.Log

/**
 * Handles session duration tracking and expiration notifications.
 *
 * @param sessionExpirationNoticeInterval The time in seconds when the session expiration notice
 * should be shown before the expiration date.
 * @param eventHandler Handler for emitting events to the messaging client.
 * @param log Logger instance for logging session duration events.
 * @param getCurrentTimestamp Function to get the current timestamp in milliseconds.
 */
internal class SessionDurationHandler(
    private val sessionExpirationNoticeInterval: Long,
    private val eventHandler: EventHandler,
    private val log: Log,
    private val getCurrentTimestamp: () -> Long = { Platform().epochMillis() },
) {

    private var currentDurationSeconds: Long? = null
    private var currentExpirationDate: Long? = null

    private val expirationTimer: ActionTimer = ActionTimer(
        log = log,
        action = { emitSessionExpirationNotice() }
    )

    /**
     * Updates the session duration parameters.
     *
     * @param durationSeconds The time of inactivity (in seconds) before the session expires.
     * @param expirationDate The current expiration date for the session (timestamp).
     */
    fun updateSessionDuration(durationSeconds: Long?, expirationDate: Long?) {
        log.i { "updateSessionDuration(durationSeconds=$durationSeconds, expirationDate=$expirationDate)" }

        if (durationSeconds != null && durationSeconds != currentDurationSeconds) {
            log.i { "Duration seconds changed from $currentDurationSeconds to $durationSeconds" }
            currentDurationSeconds = durationSeconds
            emitSessionDurationEvent(durationSeconds)
        }

        if (expirationDate != null && expirationDate != currentExpirationDate) {
            log.i { "Expiration date changed from $currentExpirationDate to $expirationDate" }
            currentExpirationDate = expirationDate
            handleExpirationDateChange(expirationDate)
        }
    }

    private fun emitSessionDurationEvent(durationSeconds: Long) {
        log.i { "Emitting SessionDuration event with duration: $durationSeconds seconds" }
        eventHandler.onEvent(Event.SessionDuration(durationSeconds))
    }

    private fun handleExpirationDateChange(expirationDate: Long) {
        val noticeTimeSeconds = expirationDate - sessionExpirationNoticeInterval
        val noticeTimeMillis = noticeTimeSeconds * 1000
        val currentTimeMillis = getCurrentTimestamp()
        val delayMillis = noticeTimeMillis - currentTimeMillis

        if (delayMillis > 0) {
            log.i { "Starting expiration timer with delay: $delayMillis ms (${delayMillis / 1000} seconds)" }
            expirationTimer.start(delayMillis)
        } else {
            log.w { "Notice time has already passed (delay: $delayMillis ms)" }
        }
    }
    /**
     * Emits a SessionExpirationNotice event to the messaging client.
     */
    private fun emitSessionExpirationNotice() {
        log.i { "Emitting SessionExpirationNotice event" }
        eventHandler.onEvent(Event.SessionExpirationNotice)
    }

    fun clear() {
        currentDurationSeconds = null
        currentExpirationDate = null
        expirationTimer.cancel()
    }
}

