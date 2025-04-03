package com.genesys.cloud.messenger.transport.core.sessionduration

import com.genesys.cloud.messenger.transport.core.Configuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object SessionDurationUseCase {

    private val _sessionDurationStateFlow = MutableStateFlow(SessionDurationState())
    internal val sessionDurationStateFlow = _sessionDurationStateFlow.asStateFlow()

    /**
     * Updates the session expiration notice interval.
     *
     * @param newSessionExpirationNoticeInterval The new session expiration notice interval in
     * seconds. The value should not be negative. Values below zero will fall back to
     * [Configuration.DEFAULT_INTERVAL]
     */
    internal fun updateSessionExpirationNoticeInterval(newSessionExpirationNoticeInterval: Long) {
        val validInterval = if (newSessionExpirationNoticeInterval < 0) {
            Configuration.DEFAULT_INTERVAL
        } else
            newSessionExpirationNoticeInterval

        if (validInterval != _sessionDurationStateFlow.value.sessionExpirationNoticeInterval) {
            _sessionDurationStateFlow.update {
                it.copy(sessionExpirationNoticeInterval = validInterval)
            }
        }

    }
}

internal data class SessionDurationState(
    val sessionExpirationNoticeInterval: Long = Configuration.DEFAULT_INTERVAL
)
