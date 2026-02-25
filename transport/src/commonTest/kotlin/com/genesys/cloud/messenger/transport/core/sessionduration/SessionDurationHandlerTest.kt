package com.genesys.cloud.messenger.transport.core.sessionduration

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.EventHandler
import com.genesys.cloud.messenger.transport.util.EXPIRATION_HEALTH_CHECK_BUFFER_MILLIS
import com.genesys.cloud.messenger.transport.util.logs.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDurationHandlerTest {
    private companion object {
        const val TIMER_MARGIN_MILLIS = 100L
    }

    private var capturedEvent: Event? = null
    private var currentTime: Long = 1000000000L // Initial timestamp in seconds
    private var healthCheckTriggered: Boolean = false

    private val mockEventHandler =
        object : EventHandler {
            override var eventListener: ((Event) -> Unit)? = null

            override fun onEvent(event: Event) {
                capturedEvent = event
            }
        }

    private val mockLog =
        Log(
            enableLogs = false,
            tag = "SessionDurationHandlerTest"
        )

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var subject: SessionDurationHandler

    @BeforeTest
    fun setUp() {
        capturedEvent = null
        currentTime = 1000000000L
        healthCheckTriggered = false
        subject =
            SessionDurationHandler(
                sessionExpirationNoticeIntervalSeconds = 60L,
                healthCheckPreNoticeTimeMillis = 500L,
                eventHandler = mockEventHandler,
                log = mockLog,
                triggerHealthCheck = { healthCheckTriggered = true },
                getCurrentTimestamp = { currentTime * 1000 },
                dispatcher = testScope
            )
    }

    private fun createSubject(
        sessionExpirationNoticeIntervalSeconds: Long = 60L,
        healthCheckLeadTimeMillis: Long = 500L,
        getCurrentTime: () -> Long = { currentTime * 1000 },
        onHealthCheckTriggered: () -> Unit = { healthCheckTriggered = true }
    ) = SessionDurationHandler(
        sessionExpirationNoticeIntervalSeconds = sessionExpirationNoticeIntervalSeconds,
        healthCheckPreNoticeTimeMillis = healthCheckLeadTimeMillis,
        eventHandler = mockEventHandler,
        log = mockLog,
        triggerHealthCheck = onHealthCheckTriggered,
        getCurrentTimestamp = getCurrentTime,
        dispatcher = testScope
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
    fun `when updateSessionDuration with new expirationDate then schedules timer`() =
        testScope.runTest {
            val givenNoticeIntervalSeconds = 60L
            val givenTimeToExpirationSeconds = 300L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds)

            subject.updateSessionDuration(null, givenExpirationDate)

            assertThat(capturedEvent).isNull()
        }

    @Test
    fun `when timer expires then emits SessionExpirationNotice event with time to expiration`() =
        testScope.runTest {
            val givenNoticeIntervalSeconds = 1L
            val givenTimeToExpirationSeconds = 2L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds)

            subject.updateSessionDuration(null, givenExpirationDate)

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            advanceTimeBy(noticeDelayMillis + TIMER_MARGIN_MILLIS)

            val capturedExpirationNotice = capturedEvent as? Event.SessionExpirationNotice
            assertThat(capturedExpirationNotice).isEqualTo(Event.SessionExpirationNotice(givenTimeToExpirationSeconds))
        }

    @Test
    fun `when clear then resets state and cancels timer`() =
        testScope.runTest {
            val givenNoticeIntervalSeconds = 1L
            val givenTimeToExpirationSeconds = 2L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds)

            subject.updateSessionDuration(3600L, givenExpirationDate)
            capturedEvent = null

            subject.clear()

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            advanceTimeBy(noticeDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(capturedEvent).isNull()
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
    fun `when updateSessionDuration multiple times with different expirationDates then only last timer is active`() =
        testScope.runTest {
            val givenNoticeIntervalSeconds = 1L
            val givenFirstTimeToExpirationSeconds = 2L
            val givenSecondTimeToExpirationSeconds = 10L
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds)

            subject.updateSessionDuration(null, currentTime + givenFirstTimeToExpirationSeconds)
            subject.updateSessionDuration(null, currentTime + givenSecondTimeToExpirationSeconds)

            val firstNoticeDelayMillis = (givenFirstTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            advanceTimeBy(firstNoticeDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(capturedEvent).isNull()
        }

    @Test
    fun `when updateSessionDuration with null expirationDate then does not schedule timer`() {
        subject.updateSessionDuration(null, null)

        assertThat(capturedEvent).isNull()
    }

    @Test
    fun `when health check timer fires then triggers health check callback`() =
        testScope.runTest {
            val givenNoticeIntervalSeconds = 1L
            val givenHealthCheckLeadTimeMillis = 500L
            val givenTimeToExpirationSeconds = 3L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject =
                createSubject(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds,
                    healthCheckLeadTimeMillis = givenHealthCheckLeadTimeMillis
                )

            subject.updateSessionDuration(null, givenExpirationDate)

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            val healthCheckDelayMillis = noticeDelayMillis - givenHealthCheckLeadTimeMillis
            advanceTimeBy(healthCheckDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(healthCheckTriggered).isTrue()
        }

    @Test
    fun `when health check lead time is too short then triggers health check immediately`() =
        testScope.runTest {
            val givenNoticeIntervalSeconds = 1L
            val givenHealthCheckLeadTimeMillis = 5000L
            val givenTimeToExpirationSeconds = 2L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject =
                createSubject(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds,
                    healthCheckLeadTimeMillis = givenHealthCheckLeadTimeMillis
                )

            subject.updateSessionDuration(null, givenExpirationDate)

            assertThat(healthCheckTriggered).isTrue()
        }

    @Test
    fun `when updateSessionDuration with new expirationDate then reschedules both timers`() =
        testScope.runTest {
            val givenNoticeIntervalSeconds = 1L
            val givenHealthCheckLeadTimeMillis = 300L
            val givenFirstTimeToExpirationSeconds = 2L
            val givenSecondTimeToExpirationSeconds = 10L
            healthCheckTriggered = false
            val subject =
                createSubject(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds,
                    healthCheckLeadTimeMillis = givenHealthCheckLeadTimeMillis
                )

            subject.updateSessionDuration(null, currentTime + givenFirstTimeToExpirationSeconds)
            subject.updateSessionDuration(null, currentTime + givenSecondTimeToExpirationSeconds)

            val firstNoticeDelayMillis = (givenFirstTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            advanceTimeBy(firstNoticeDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(healthCheckTriggered).isFalse()
            assertThat(capturedEvent).isNull()
        }

    @Test
    fun `when clear then cancels health check timer`() =
        testScope.runTest {
            val givenNoticeIntervalSeconds = 1L
            val givenHealthCheckLeadTimeMillis = 300L
            val givenTimeToExpirationSeconds = 2L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject =
                createSubject(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds,
                    healthCheckLeadTimeMillis = givenHealthCheckLeadTimeMillis
                )

            subject.updateSessionDuration(null, givenExpirationDate)

            subject.clear()

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            advanceTimeBy(noticeDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(healthCheckTriggered).isFalse()
        }

    @Test
    fun `when triggerHealthCheck is set after construction then timer uses updated callback`() =
        testScope.runTest {
            var customCallbackTriggered = false
            val givenNoticeIntervalSeconds = 1L
            val givenHealthCheckLeadTimeMillis = 500L
            val givenTimeToExpirationSeconds = 3L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject =
                SessionDurationHandler(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds,
                    healthCheckPreNoticeTimeMillis = givenHealthCheckLeadTimeMillis,
                    eventHandler = mockEventHandler,
                    log = mockLog,
                    getCurrentTimestamp = { currentTime * 1000 },
                    dispatcher = testScope
                )

            subject.setTriggerHealthCheck { customCallbackTriggered = true }

            subject.updateSessionDuration(null, givenExpirationDate)

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            val healthCheckDelayMillis = noticeDelayMillis - givenHealthCheckLeadTimeMillis
            advanceTimeBy(healthCheckDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(customCallbackTriggered).isTrue()
        }

    @Test
    fun `when triggerHealthCheck is reassigned then new callback is used`() =
        testScope.runTest {
            var firstCallbackTriggered = false
            var secondCallbackTriggered = false
            val givenNoticeIntervalSeconds = 1L
            val givenHealthCheckLeadTimeMillis = 500L
            val givenTimeToExpirationSeconds = 3L
            val subject =
                SessionDurationHandler(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds,
                    healthCheckPreNoticeTimeMillis = givenHealthCheckLeadTimeMillis,
                    eventHandler = mockEventHandler,
                    log = mockLog,
                    getCurrentTimestamp = { currentTime * 1000 },
                    dispatcher = testScope
                )

            subject.setTriggerHealthCheck { firstCallbackTriggered = true }
            subject.setTriggerHealthCheck { secondCallbackTriggered = true }

            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            subject.updateSessionDuration(null, givenExpirationDate)

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            val healthCheckDelayMillis = noticeDelayMillis - givenHealthCheckLeadTimeMillis
            advanceTimeBy(healthCheckDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(firstCallbackTriggered).isFalse()
            assertThat(secondCallbackTriggered).isTrue()
        }

    @Test
    fun `when same expirationDate is provided twice then timer is not rescheduled`() =
        testScope.runTest {
            var healthCheckCount = 0
            val givenNoticeIntervalSeconds = 1L
            val givenHealthCheckLeadTimeMillis = 300L
            val givenTimeToExpirationSeconds = 2L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject =
                SessionDurationHandler(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds,
                    healthCheckPreNoticeTimeMillis = givenHealthCheckLeadTimeMillis,
                    eventHandler = mockEventHandler,
                    log = mockLog,
                    triggerHealthCheck = { healthCheckCount++ },
                    getCurrentTimestamp = { currentTime * 1000 },
                    dispatcher = testScope
                )

            subject.updateSessionDuration(null, givenExpirationDate)
            subject.updateSessionDuration(null, givenExpirationDate)

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            val healthCheckDelayMillis = noticeDelayMillis - givenHealthCheckLeadTimeMillis
            advanceTimeBy(healthCheckDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(healthCheckCount).isEqualTo(1)
        }

    @Test
    fun `when onMessage and session expiration notice was sent then emits RemoveSessionExpirationNotice and triggers health check`() =
        testScope.runTest {
            val givenNoticeIntervalSeconds = 1L
            val givenTimeToExpirationSeconds = 2L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds)

            subject.updateSessionDuration(null, givenExpirationDate)

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            advanceTimeBy(noticeDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(capturedEvent).isEqualTo(Event.SessionExpirationNotice(givenTimeToExpirationSeconds))

            capturedEvent = null
            healthCheckTriggered = false

            subject.onMessage()

            assertThat(capturedEvent).isEqualTo(Event.RemoveSessionExpirationNotice)
            assertThat(healthCheckTriggered).isTrue()
        }

    @Test
    fun `when onMessage and session expiration notice was not sent then does nothing`() {
        subject.updateSessionDuration(3600L, null)
        capturedEvent = null
        healthCheckTriggered = false

        subject.onMessage()

        assertThat(capturedEvent).isNull()
        assertThat(healthCheckTriggered).isEqualTo(false)
    }

    @Test
    fun `when onMessage called twice after expiration notice then only first call emits event`() =
        testScope.runTest {
            val givenNoticeIntervalSeconds = 1L
            val givenTimeToExpirationSeconds = 2L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds)

            subject.updateSessionDuration(null, givenExpirationDate)

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            advanceTimeBy(noticeDelayMillis + TIMER_MARGIN_MILLIS)

            subject.onMessage()
            assertThat(capturedEvent).isEqualTo(Event.RemoveSessionExpirationNotice)

            capturedEvent = null
            subject.onMessage()
            assertThat(capturedEvent).isNull()
        }

    @Test
    fun `when clear after expiration notice sent then resets sessionExpirationNoticeSent flag`() =
        testScope.runTest {
            val givenNoticeIntervalSeconds = 1L
            val givenTimeToExpirationSeconds = 2L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds)

            subject.updateSessionDuration(null, givenExpirationDate)

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            advanceTimeBy(noticeDelayMillis + TIMER_MARGIN_MILLIS)

            subject.clear()

            capturedEvent = null
            healthCheckTriggered = false

            subject.onMessage()

            assertThat(capturedEvent).isNull()
            assertThat(healthCheckTriggered).isEqualTo(false)
        }

    @Test
    fun `when clearAndRemoveNotice and expiration notice was sent then emits RemoveSessionExpirationNotice and clears state`() =
        testScope.runTest {
            val givenNoticeIntervalSeconds = 1L
            val givenTimeToExpirationSeconds = 2L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds)

            subject.updateSessionDuration(null, givenExpirationDate)

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            advanceTimeBy(noticeDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(capturedEvent).isEqualTo(Event.SessionExpirationNotice(givenTimeToExpirationSeconds))

            capturedEvent = null

            subject.clearAndRemoveNotice()

            assertThat(capturedEvent).isEqualTo(Event.RemoveSessionExpirationNotice)
        }

    @Test
    fun `when clearAndRemoveNotice and expiration notice was not sent then does not emit event but clears state`() {
        subject.updateSessionDuration(3600L, null)
        capturedEvent = null

        subject.clearAndRemoveNotice()

        assertThat(capturedEvent).isNull()
    }

    @Test
    fun `when clearAndRemoveNotice then subsequent onMessage does not emit RemoveSessionExpirationNotice`() =
        testScope.runTest {
            val givenNoticeIntervalSeconds = 1L
            val givenTimeToExpirationSeconds = 2L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds)

            subject.updateSessionDuration(null, givenExpirationDate)

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            advanceTimeBy(noticeDelayMillis + TIMER_MARGIN_MILLIS)

            subject.clearAndRemoveNotice()

            capturedEvent = null
            healthCheckTriggered = false

            subject.onMessage()

            assertThat(capturedEvent).isNull()
            assertThat(healthCheckTriggered).isFalse()
        }

    @Test
    fun `when clearAndRemoveNotice then cancels pending timers`() =
        testScope.runTest {
            val givenNoticeIntervalSeconds = 1L
            val givenTimeToExpirationSeconds = 3L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds)

            subject.updateSessionDuration(null, givenExpirationDate)

            subject.clearAndRemoveNotice()

            capturedEvent = null

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            advanceTimeBy(noticeDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(capturedEvent).isNull()
        }

    @Test
    fun `when notice time has already passed then emits SessionExpirationNotice immediately`() =
        testScope.runTest {
            val givenNoticeIntervalSeconds = 60L
            val givenTimeToExpirationSeconds = 30L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds)

            subject.updateSessionDuration(null, givenExpirationDate)

            val capturedExpirationNotice = capturedEvent as? Event.SessionExpirationNotice
            assertThat(capturedExpirationNotice).isEqualTo(Event.SessionExpirationNotice(givenTimeToExpirationSeconds))
        }

    @Test
    fun `when clearAndRemoveNotice called multiple times then only first call emits event`() =
        testScope.runTest {
            var eventCount = 0
            val countingEventHandler =
                object : EventHandler {
                    override var eventListener: ((Event) -> Unit)? = null

                    override fun onEvent(event: Event) {
                        if (event is Event.RemoveSessionExpirationNotice) {
                            eventCount++
                        }
                        capturedEvent = event
                    }
                }
            val givenNoticeIntervalSeconds = 1L
            val givenHealthCheckLeadTimeMillis = 300L
            val givenTimeToExpirationSeconds = 2L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject =
                SessionDurationHandler(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds,
                    healthCheckPreNoticeTimeMillis = givenHealthCheckLeadTimeMillis,
                    eventHandler = countingEventHandler,
                    log = mockLog,
                    triggerHealthCheck = { healthCheckTriggered = true },
                    getCurrentTimestamp = { currentTime * 1000 },
                    dispatcher = testScope
                )

            subject.updateSessionDuration(null, givenExpirationDate)

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            advanceTimeBy(noticeDelayMillis + TIMER_MARGIN_MILLIS)

            subject.clearAndRemoveNotice()
            subject.clearAndRemoveNotice()
            subject.clearAndRemoveNotice()

            assertThat(eventCount).isEqualTo(1)
        }

    @Test
    fun `when onMessage then calculates updatedExpirationDate based on current time and duration`() =
        testScope.runTest {
            val givenDurationSeconds = 3600L
            val givenNoticeIntervalSeconds = 1L
            val givenTimeToExpirationSeconds = 10L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds)

            subject.updateSessionDuration(givenDurationSeconds, givenExpirationDate)
            capturedEvent = null

            subject.onMessage()

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            advanceTimeBy(noticeDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(healthCheckTriggered).isFalse()
        }

    @Test
    fun `when health check timer fires and session was extended by onMessage then reschedules timers`() =
        testScope.runTest {
            val givenDurationSeconds = 100L
            val givenNoticeIntervalSeconds = 1L
            val givenHealthCheckLeadTimeMillis = 500L
            val givenTimeToExpirationSeconds = 3L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject =
                createSubject(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds,
                    healthCheckLeadTimeMillis = givenHealthCheckLeadTimeMillis
                )

            subject.updateSessionDuration(givenDurationSeconds, givenExpirationDate)

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            val healthCheckDelayMillis = noticeDelayMillis - givenHealthCheckLeadTimeMillis
            val messageActivityTimeMillis = healthCheckDelayMillis - 500L
            advanceTimeBy(messageActivityTimeMillis)
            subject.onMessage()

            advanceTimeBy(givenHealthCheckLeadTimeMillis + TIMER_MARGIN_MILLIS)

            assertThat(healthCheckTriggered).isFalse()
        }

    @Test
    fun `when health check timer fires and no message activity then triggers health check`() =
        testScope.runTest {
            val givenNoticeIntervalSeconds = 1L
            val givenHealthCheckLeadTimeMillis = 500L
            val givenTimeToExpirationSeconds = 3L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject =
                createSubject(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds,
                    healthCheckLeadTimeMillis = givenHealthCheckLeadTimeMillis
                )

            subject.updateSessionDuration(null, givenExpirationDate)

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            val healthCheckDelayMillis = noticeDelayMillis - givenHealthCheckLeadTimeMillis
            advanceTimeBy(healthCheckDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(healthCheckTriggered).isTrue()
        }

    @Test
    fun `when expiration timer fires and session was extended by onMessage then reschedules timers`() =
        testScope.runTest {
            val givenDurationSeconds = 100L
            val givenNoticeIntervalSeconds = 1L
            val givenTimeToExpirationSeconds = 2L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds)

            subject.updateSessionDuration(givenDurationSeconds, givenExpirationDate)
            capturedEvent = null

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            val messageActivityTimeMillis = noticeDelayMillis / 2
            advanceTimeBy(messageActivityTimeMillis)
            subject.onMessage()

            advanceTimeBy(noticeDelayMillis - messageActivityTimeMillis + TIMER_MARGIN_MILLIS)

            assertThat(capturedEvent).isNull()
        }

    @Test
    fun `when expiration timer fires and no message activity then emits expiration notice`() =
        testScope.runTest {
            val givenNoticeIntervalSeconds = 1L
            val givenTimeToExpirationSeconds = 2L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds)

            subject.updateSessionDuration(null, givenExpirationDate)
            capturedEvent = null

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            advanceTimeBy(noticeDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(capturedEvent).isEqualTo(Event.SessionExpirationNotice(givenTimeToExpirationSeconds))
        }

    @Test
    fun `when onMessage without durationSeconds set then does not calculate updatedExpirationDate`() =
        testScope.runTest {
            val givenNoticeIntervalSeconds = 1L
            val givenHealthCheckLeadTimeMillis = 500L
            val givenTimeToExpirationSeconds = 3L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject =
                createSubject(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds,
                    healthCheckLeadTimeMillis = givenHealthCheckLeadTimeMillis
                )

            subject.updateSessionDuration(null, givenExpirationDate)

            subject.onMessage()

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            val healthCheckDelayMillis = noticeDelayMillis - givenHealthCheckLeadTimeMillis
            advanceTimeBy(healthCheckDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(healthCheckTriggered).isTrue()
        }

    @Test
    fun `when clear then resets updatedExpirationDate`() =
        testScope.runTest {
            val givenDurationSeconds = 100L
            val givenNoticeIntervalSeconds = 1L
            val givenHealthCheckLeadTimeMillis = 500L
            val givenTimeToExpirationSeconds = 3L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject =
                createSubject(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds,
                    healthCheckLeadTimeMillis = givenHealthCheckLeadTimeMillis
                )

            subject.updateSessionDuration(givenDurationSeconds, givenExpirationDate)
            subject.onMessage()

            subject.clear()

            subject.updateSessionDuration(null, givenExpirationDate)

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            val healthCheckDelayMillis = noticeDelayMillis - givenHealthCheckLeadTimeMillis
            advanceTimeBy(healthCheckDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(healthCheckTriggered).isTrue()
        }

    @Test
    fun `when expiration timer fires and updatedExpirationDate is greater than currentExpirationDate then reschedules with updatedExpirationDate`() =
        testScope.runTest {
            val givenDurationSeconds = 100L
            val givenNoticeIntervalSeconds = 1L
            val givenHealthCheckLeadTimeMillis = 0L
            val givenTimeToExpirationSeconds = 2L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject =
                createSubject(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds,
                    healthCheckLeadTimeMillis = givenHealthCheckLeadTimeMillis
                )

            subject.updateSessionDuration(givenDurationSeconds, givenExpirationDate)

            subject.onMessage()

            capturedEvent = null

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            advanceTimeBy(noticeDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(capturedEvent).isNull()
        }

    @Test
    fun `when expiration notice is emitted then schedules health check at expiration time plus buffer`() =
        testScope.runTest {
            val givenNoticeIntervalSeconds = 1L
            val givenTimeToExpirationSeconds = 2L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds)

            subject.updateSessionDuration(null, givenExpirationDate)

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            advanceTimeBy(noticeDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(capturedEvent).isEqualTo(Event.SessionExpirationNotice(givenTimeToExpirationSeconds))
            healthCheckTriggered = false

            val healthCheckDelayMillis = givenTimeToExpirationSeconds * 1000 + EXPIRATION_HEALTH_CHECK_BUFFER_MILLIS
            advanceTimeBy(healthCheckDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(healthCheckTriggered).isTrue()
        }

    @Test
    fun `when notice time has already passed then schedules health check based on remaining time plus buffer`() =
        testScope.runTest {
            val givenNoticeIntervalSeconds = 60L
            val givenTimeToExpirationSeconds = 30L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds)

            subject.updateSessionDuration(null, givenExpirationDate)

            assertThat(capturedEvent).isEqualTo(Event.SessionExpirationNotice(givenTimeToExpirationSeconds))
            healthCheckTriggered = false

            val healthCheckDelayMillis = givenTimeToExpirationSeconds * 1000 + EXPIRATION_HEALTH_CHECK_BUFFER_MILLIS
            advanceTimeBy(healthCheckDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(healthCheckTriggered).isTrue()
        }

    @Test
    fun `when session already expired then schedules health check with buffer only`() =
        testScope.runTest {
            val givenNoticeIntervalSeconds = 60L
            val givenExpirationDate = currentTime 
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds)

            subject.updateSessionDuration(null, givenExpirationDate)

            assertThat(capturedEvent).isEqualTo(Event.SessionExpirationNotice(0L))
            healthCheckTriggered = false

            advanceTimeBy(EXPIRATION_HEALTH_CHECK_BUFFER_MILLIS + TIMER_MARGIN_MILLIS)

            assertThat(healthCheckTriggered).isTrue()
        }

    @Test
    fun `when clear then cancels expiration health check timer`() =
        testScope.runTest {
            val givenNoticeIntervalSeconds = 1L
            val givenTimeToExpirationSeconds = 2L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds)

            subject.updateSessionDuration(null, givenExpirationDate)

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            advanceTimeBy(noticeDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(capturedEvent).isEqualTo(Event.SessionExpirationNotice(givenTimeToExpirationSeconds))
            healthCheckTriggered = false

            subject.clear()

            val healthCheckDelayMillis = givenTimeToExpirationSeconds * 1000 + EXPIRATION_HEALTH_CHECK_BUFFER_MILLIS
            advanceTimeBy(healthCheckDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(healthCheckTriggered).isFalse()
        }

    @Test
    fun `when onMessage after expiration notice then cancels expiration health check timer`() =
        testScope.runTest {
            var healthCheckCount = 0
            val givenNoticeIntervalSeconds = 1L
            val givenTimeToExpirationSeconds = 2L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject =
                SessionDurationHandler(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds,
                    healthCheckPreNoticeTimeMillis = 300L,
                    eventHandler = mockEventHandler,
                    log = mockLog,
                    triggerHealthCheck = { healthCheckCount++ },
                    getCurrentTimestamp = { currentTime * 1000 },
                    dispatcher = testScope
                )

            subject.updateSessionDuration(3600L, givenExpirationDate)

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            advanceTimeBy(noticeDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(capturedEvent).isEqualTo(Event.SessionExpirationNotice(givenTimeToExpirationSeconds))

            val healthCheckCountAfterNotice = healthCheckCount
            subject.onMessage()

            val expectedHealthCheckCount = healthCheckCountAfterNotice + 1
            assertThat(healthCheckCount).isEqualTo(expectedHealthCheckCount)

            val healthCheckDelayMillis = givenTimeToExpirationSeconds * 1000 + EXPIRATION_HEALTH_CHECK_BUFFER_MILLIS
            advanceTimeBy(healthCheckDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(healthCheckCount).isEqualTo(expectedHealthCheckCount)
        }

    @Test
    fun `when clearAndRemoveNotice then cancels expiration health check timer`() =
        testScope.runTest {
            val givenNoticeIntervalSeconds = 1L
            val givenTimeToExpirationSeconds = 2L
            val givenExpirationDate = currentTime + givenTimeToExpirationSeconds
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeIntervalSeconds)

            subject.updateSessionDuration(null, givenExpirationDate)

            val noticeDelayMillis = (givenTimeToExpirationSeconds - givenNoticeIntervalSeconds) * 1000
            advanceTimeBy(noticeDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(capturedEvent).isEqualTo(Event.SessionExpirationNotice(givenTimeToExpirationSeconds))
            healthCheckTriggered = false

            subject.clearAndRemoveNotice()

            val healthCheckDelayMillis = givenTimeToExpirationSeconds * 1000 + EXPIRATION_HEALTH_CHECK_BUFFER_MILLIS
            advanceTimeBy(healthCheckDelayMillis + TIMER_MARGIN_MILLIS)

            assertThat(healthCheckTriggered).isFalse()
        }
}
