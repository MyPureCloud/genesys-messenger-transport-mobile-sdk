package com.genesys.cloud.messenger.transport.core.sessionduration

import com.genesys.cloud.messenger.transport.core.Configuration
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class SessionDurationUseCaseTest {

    @Test
    fun `SessionDurationUseCase should have default value for the interval`() =
        runTest {
            assertEquals(
                Configuration.DEFAULT_INTERVAL,
                SessionDurationUseCase.sessionDurationStateFlow.value.sessionExpirationNoticeInterval
            )
        }

    @Test
    fun `updateSessionExpirationNoticeInterval should update the interval`() =
        runTest {
            val newInterval = 100L

            SessionDurationUseCase.updateSessionExpirationNoticeInterval(newInterval)
            assertEquals(
                newInterval,
                SessionDurationUseCase.sessionDurationStateFlow.value.sessionExpirationNoticeInterval
            )
        }

    @Test
    fun `updateSessionExpirationNoticeInterval should fall back to default value in case of negative parameter value`() =
        runTest {
            val newInterval = -100L

            SessionDurationUseCase.updateSessionExpirationNoticeInterval(newInterval)
            assertEquals(
                Configuration.DEFAULT_INTERVAL,
                SessionDurationUseCase.sessionDurationStateFlow.value.sessionExpirationNoticeInterval
            )
        }
}
