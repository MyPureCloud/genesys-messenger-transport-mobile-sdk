package com.genesys.cloud.messenger.transport.core.sessionduration

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.EventHandler
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.utility.DEFAULT_TIMEOUT
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.BeforeTest
import kotlin.test.Test

class SessionDurationHandlerTest {
    private var capturedEvent: Event? = null
    private var currentTime: Long = 1000000000L // Initial timestamp in seconds
    
    private val mockEventHandler = object : EventHandler {
        override var eventListener: ((Event) -> Unit)? = null
        
        override fun onEvent(event: Event) {
            capturedEvent = event
        }
    }
    
    private val mockLog = Log(
        enableLogs = false,
        tag = "SessionDurationHandlerTest"
    )

    private lateinit var subject: SessionDurationHandler

    @BeforeTest
    fun setUp() {
        capturedEvent = null
        currentTime = 1000000000L
        subject = SessionDurationHandler(
            sessionExpirationNoticeInterval = 60L,
            eventHandler = mockEventHandler,
            log = mockLog,
            getCurrentTimestamp = { currentTime * 1000 }
        )
    }

    private fun createSubject(
        sessionExpirationNoticeInterval: Long = 60L,
        getCurrentTime: () -> Long = { currentTime * 1000 }
    ) = SessionDurationHandler(
        sessionExpirationNoticeInterval = sessionExpirationNoticeInterval,
        eventHandler = mockEventHandler,
        log = mockLog,
        getCurrentTimestamp = getCurrentTime
    )

    @Test
    fun `when updateSessionDuration with new durationSeconds then emits SessionDuration event`() {
        val givenDurationSeconds = 3600L

        subject.updateSessionDuration(givenDurationSeconds, null)

        val expectedEvent = Event.SessionDuration(givenDurationSeconds)
        assertThat(capturedEvent).isEqualTo(expectedEvent)
    }

    @Test
    fun `when updateSessionDuration with same durationSeconds then does not emit event`() {
        val givenDurationSeconds = 3600L
        
        subject.updateSessionDuration(givenDurationSeconds, null)
        capturedEvent = null
        subject.updateSessionDuration(givenDurationSeconds, null)

        assertThat(capturedEvent).isNull()
    }

    @Test
    fun `when updateSessionDuration with different durationSeconds then emits new event`() {
        subject.updateSessionDuration(3600L, null)
        capturedEvent = null

        val givenNewDuration = 7200L
        subject.updateSessionDuration(givenNewDuration, null)

        val expectedEvent = Event.SessionDuration(givenNewDuration)
        assertThat(capturedEvent).isEqualTo(expectedEvent)
    }

    @Test
    fun `when updateSessionDuration with null durationSeconds then does not emit event`() {
        subject.updateSessionDuration(null, null)

        assertThat(capturedEvent).isNull()
    }

    @Test
    fun `when updateSessionDuration with new expirationDate then schedules timer`() = runBlocking {
        val givenNoticeInterval = 60L // 60 seconds
        val givenExpirationDate = 1000000300L // Current time + 300 seconds
        val subject = createSubject(sessionExpirationNoticeInterval = givenNoticeInterval)

        subject.updateSessionDuration(null, givenExpirationDate)

        // Timer should be scheduled but not expired yet
        assertThat(capturedEvent).isNull()
    }

    @Test
    fun `when timer expires then emits SessionExpirationNotice event`() = runBlocking {
        withTimeout(DEFAULT_TIMEOUT) {
            val givenNoticeInterval = 1L
            val givenExpirationDate = currentTime + 2L
            val subject = createSubject(sessionExpirationNoticeInterval = givenNoticeInterval)

            subject.updateSessionDuration(null, givenExpirationDate)
            
            delay(1200)

            val expectedEvent = Event.SessionExpirationNotice
            assertThat(capturedEvent).isEqualTo(expectedEvent)
        }
    }


    @Test
    fun `when clear then resets state and cancels timer`() = runBlocking {
        withTimeout(DEFAULT_TIMEOUT) {
            val givenNoticeInterval = 1L
            val givenExpirationDate = currentTime + 2L
            val subject = createSubject(sessionExpirationNoticeInterval = givenNoticeInterval)

            subject.updateSessionDuration(3600L, givenExpirationDate)
            capturedEvent = null

            subject.clear()

            // Wait to ensure timer would have fired if not cancelled
            kotlinx.coroutines.delay(1500)

            // Timer should be cancelled, so no event should be emitted
            assertThat(capturedEvent).isNull()
        }
    }

    @Test
    fun `when updateSessionDuration after clear then emits event again`() {
        val givenDurationSeconds = 7200L

        subject.updateSessionDuration(3600L, null)
        capturedEvent = null
        subject.clear()
        subject.updateSessionDuration(givenDurationSeconds, null)

        val expectedEvent = Event.SessionDuration(givenDurationSeconds)
        assertThat(capturedEvent).isEqualTo(expectedEvent)
    }

    @Test
    fun `when updateSessionDuration multiple times with different expirationDates then only last timer is active`() = runBlocking {
        withTimeout(DEFAULT_TIMEOUT) {
            val givenNoticeInterval = 1L
            val subject = createSubject(sessionExpirationNoticeInterval = givenNoticeInterval)

            subject.updateSessionDuration(null, currentTime + 2L)
            subject.updateSessionDuration(null, currentTime + 10L)
            
            // Wait for when first timer would have expired
            delay(1200)

            // First timer should be cancelled, so no event
            assertThat(capturedEvent).isNull()
        }
    }

    @Test
    fun `when updateSessionDuration with null expirationDate then does not schedule timer`() {
        subject.updateSessionDuration(null, null)

        assertThat(capturedEvent).isNull()
    }
}

