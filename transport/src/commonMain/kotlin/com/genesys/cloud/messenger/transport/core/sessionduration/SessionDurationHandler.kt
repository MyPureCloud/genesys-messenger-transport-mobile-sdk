package com.genesys.cloud.messenger.transport.core.sessionduration

/**
 * Handles session duration tracking and expiration notifications.
 *
 * @param sessionExpirationNoticeInterval The time in seconds when the session expiration notice
 * should be shown before the expiration date.
 */
internal class SessionDurationHandler(
    private val sessionExpirationNoticeInterval: Long
) {

    /**
     * Updates the session duration parameters.
     *
     * @param durationSeconds The time of inactivity (in seconds) before the session expires.
     * @param expirationDate The current expiration date for the session (timestamp).
     */
    fun updateSession(durationSeconds: Long?, expirationDate: Long?) {
        // Implementation will be done later
    }

    /**
     * Clears the session duration tracking.
     */
    fun clear() {
        // Implementation will be done later
    }
}

