package com.genesys.cloud.messenger.transport.core.sessionduration

import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.EventHandler
import com.genesys.cloud.messenger.transport.util.logs.Log

/**
 * Handles session duration tracking and expiration notifications.
 *
 * @param sessionExpirationNoticeInterval The time in seconds when the session expiration notice
 * should be shown before the expiration date.
 * @param eventHandler Handler for emitting events to the messaging client.
 * @param log Logger instance for logging session duration events.
 */
internal class SessionDurationHandler(
    private val sessionExpirationNoticeInterval: Long,
    private val eventHandler: EventHandler,
    private val log: Log,
) {

    private var currentDurationSeconds: Long? = null
    private var currentExpirationDate: Long? = null

    /**
     * Updates the session duration parameters.
     *
     * @param durationSeconds The time of inactivity (in seconds) before the session expires.
     * @param expirationDate The current expiration date for the session (timestamp).
     */
    fun updateSessionDuration(durationSeconds: Long?, expirationDate: Long?) {
        log.i { "updateSessionDuration(durationSeconds=$durationSeconds, expirationDate=$expirationDate)" }

        // Check if duration seconds has changed
        if (durationSeconds != null && durationSeconds != currentDurationSeconds) {
            log.i { "Duration seconds changed from $currentDurationSeconds to $durationSeconds" }
            currentDurationSeconds = durationSeconds
        }

        // Check if expiration date has changed
        if (expirationDate != null && expirationDate != currentExpirationDate) {
            log.i { "Expiration date changed from $currentExpirationDate to $expirationDate" }
            currentExpirationDate = expirationDate
            handleExpirationDateChange(expirationDate)
        }
    }

    private fun handleExpirationDateChange(expirationDate: Long) {
        // Implementation will be done later
    }

    fun clear() {
        log.i { "Clearing session duration handler" }
        currentDurationSeconds = null
        currentExpirationDate = null
        // Additional cleanup will be done later
    }
}

