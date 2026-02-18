package com.genesys.cloud.messenger.transport.core.sessionduration

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.EventHandler
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
            val givenNoticeInterval = 60L // 60 seconds
            val givenExpirationDate = 1000000300L // Current time + 300 seconds
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeInterval)

            subject.updateSessionDuration(null, givenExpirationDate)

            // Timer should be scheduled but not expired yet
            assertThat(capturedEvent).isNull()
        }

    @Test
    fun `when timer expires then emits SessionExpirationNotice event with time to expiration`() =
        testScope.runTest {
            val givenNoticeInterval = 1L
            val givenExpirationDate = currentTime + 2L
            val expectedTimeToExpiration = givenExpirationDate - currentTime
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeInterval)

            subject.updateSessionDuration(null, givenExpirationDate)

            advanceTimeBy(1100)

            val capturedExpirationNotice = capturedEvent as? Event.SessionExpirationNotice
            assertThat(capturedExpirationNotice).isEqualTo(Event.SessionExpirationNotice(expectedTimeToExpiration))
        }

    @Test
    fun `when clear then resets state and cancels timer`() =
        testScope.runTest {
            val givenNoticeInterval = 1L
            val givenExpirationDate = currentTime + 2L
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeInterval)

            subject.updateSessionDuration(3600L, givenExpirationDate)
            capturedEvent = null

            subject.clear()

            // Advance time past when timer would have fired
            advanceTimeBy(1500)

            // Timer should be cancelled, so no event should be emitted
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
            val givenNoticeInterval = 1L
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeInterval)

            subject.updateSessionDuration(null, currentTime + 2L)
            subject.updateSessionDuration(null, currentTime + 10L)

            // Advance time past when first timer would have expired
            advanceTimeBy(1200)

            // First timer should be cancelled, so no event
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
            val givenNoticeInterval = 1L
            val givenHealthCheckLeadTime = 500L
            val givenExpirationDate = currentTime + 3L
            val subject =
                createSubject(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeInterval,
                    healthCheckLeadTimeMillis = givenHealthCheckLeadTime
                )

            subject.updateSessionDuration(null, givenExpirationDate)

            // Wait for health check timer to fire (should fire 500ms before expiration notice)
            advanceTimeBy(1600)

            assertThat(healthCheckTriggered).isTrue()
        }

    @Test
    fun `when health check lead time is too short then triggers health check immediately`() =
        testScope.runTest {
            val givenNoticeInterval = 1L
            val givenHealthCheckLeadTime = 5000L // 5 seconds lead time
            val givenExpirationDate = currentTime + 2L // Only 1 second until notice time
            val subject =
                createSubject(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeInterval,
                    healthCheckLeadTimeMillis = givenHealthCheckLeadTime
                )

            subject.updateSessionDuration(null, givenExpirationDate)

            // Health check should be triggered immediately since lead time is too short
            assertThat(healthCheckTriggered).isTrue()
        }

    @Test
    fun `when updateSessionDuration with new expirationDate then reschedules both timers`() =
        testScope.runTest {
            val givenNoticeInterval = 1L
            val givenHealthCheckLeadTime = 300L
            healthCheckTriggered = false
            val subject =
                createSubject(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeInterval,
                    healthCheckLeadTimeMillis = givenHealthCheckLeadTime
                )

            // Set initial expiration date
            subject.updateSessionDuration(null, currentTime + 2L)

            // Update to a later expiration date before timers fire
            subject.updateSessionDuration(null, currentTime + 10L)

            // Advance time past when first timers would have expired
            advanceTimeBy(1200)

            // Neither timer should have fired yet since they were rescheduled
            assertThat(healthCheckTriggered).isFalse()
            assertThat(capturedEvent).isNull()
        }

    @Test
    fun `when clear then cancels health check timer`() =
        testScope.runTest {
            val givenNoticeInterval = 1L
            val givenHealthCheckLeadTime = 300L
            val givenExpirationDate = currentTime + 2L
            val subject =
                createSubject(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeInterval,
                    healthCheckLeadTimeMillis = givenHealthCheckLeadTime
                )

            subject.updateSessionDuration(null, givenExpirationDate)

            subject.clear()

            // Advance time past when health check timer would have fired
            advanceTimeBy(1000)

            assertThat(healthCheckTriggered).isFalse()
        }

    @Test
    fun `when triggerHealthCheck is set after construction then timer uses updated callback`() =
        testScope.runTest {
            var customCallbackTriggered = false
            val givenNoticeInterval = 1L
            val givenHealthCheckLeadTime = 500L
            val givenExpirationDate = currentTime + 3L
            val subject =
                SessionDurationHandler(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeInterval,
                    healthCheckPreNoticeTimeMillis = givenHealthCheckLeadTime,
                    eventHandler = mockEventHandler,
                    log = mockLog,
                    getCurrentTimestamp = { currentTime * 1000 },
                    dispatcher = testScope
                )

            subject.setTriggerHealthCheck { customCallbackTriggered = true }

            subject.updateSessionDuration(null, givenExpirationDate)

            // Advance time for health check timer to fire
            advanceTimeBy(1600)

            assertThat(customCallbackTriggered).isTrue()
        }

    @Test
    fun `when triggerHealthCheck is reassigned then new callback is used`() =
        testScope.runTest {
            var firstCallbackTriggered = false
            var secondCallbackTriggered = false
            val givenNoticeInterval = 1L
            val givenHealthCheckLeadTime = 500L
            val subject =
                SessionDurationHandler(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeInterval,
                    healthCheckPreNoticeTimeMillis = givenHealthCheckLeadTime,
                    eventHandler = mockEventHandler,
                    log = mockLog,
                    getCurrentTimestamp = { currentTime * 1000 },
                    dispatcher = testScope
                )

            subject.setTriggerHealthCheck { firstCallbackTriggered = true }
            // Reassign to a different callback
            subject.setTriggerHealthCheck { secondCallbackTriggered = true }

            val givenExpirationDate = currentTime + 3L
            subject.updateSessionDuration(null, givenExpirationDate)

            // Advance time for health check timer to fire
            advanceTimeBy(1600)

            assertThat(firstCallbackTriggered).isFalse()
            assertThat(secondCallbackTriggered).isTrue()
        }

    @Test
    fun `when same expirationDate is provided twice then timer is not rescheduled`() =
        testScope.runTest {
            var healthCheckCount = 0
            val givenNoticeInterval = 1L
            val givenHealthCheckLeadTime = 300L
            val givenExpirationDate = currentTime + 2L
            val subject =
                SessionDurationHandler(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeInterval,
                    healthCheckPreNoticeTimeMillis = givenHealthCheckLeadTime,
                    eventHandler = mockEventHandler,
                    log = mockLog,
                    triggerHealthCheck = { healthCheckCount++ },
                    getCurrentTimestamp = { currentTime * 1000 },
                    dispatcher = testScope
                )

            subject.updateSessionDuration(null, givenExpirationDate)
            subject.updateSessionDuration(null, givenExpirationDate)

            // Advance time for timer to fire
            advanceTimeBy(1000)

            assertThat(healthCheckCount).isEqualTo(1)
        }

    @Test
    fun `when onMessage and session expiration notice was sent then emits RemoveSessionExpirationNotice and triggers health check`() =
        testScope.runTest {
            val givenNoticeInterval = 1L
            val givenExpirationDate = currentTime + 2L
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeInterval)

            subject.updateSessionDuration(null, givenExpirationDate)

            advanceTimeBy(1200)

            assertThat(capturedEvent).isEqualTo(Event.SessionExpirationNotice(givenExpirationDate - currentTime))

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
            val givenNoticeInterval = 1L
            val givenExpirationDate = currentTime + 2L
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeInterval)

            subject.updateSessionDuration(null, givenExpirationDate)

            advanceTimeBy(1200)

            subject.onMessage()
            assertThat(capturedEvent).isEqualTo(Event.RemoveSessionExpirationNotice)

            capturedEvent = null
            subject.onMessage()
            assertThat(capturedEvent).isNull()
        }

    @Test
    fun `when clear after expiration notice sent then resets sessionExpirationNoticeSent flag`() =
        testScope.runTest {
            val givenNoticeInterval = 1L
            val givenExpirationDate = currentTime + 2L
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeInterval)

            subject.updateSessionDuration(null, givenExpirationDate)

            advanceTimeBy(1200)

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
            val givenNoticeInterval = 1L
            val givenExpirationDate = currentTime + 2L
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeInterval)

            subject.updateSessionDuration(null, givenExpirationDate)

            // Advance time for expiration notice to be sent
            advanceTimeBy(1200)

            assertThat(capturedEvent).isEqualTo(Event.SessionExpirationNotice(givenExpirationDate - currentTime))

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
            val givenNoticeInterval = 1L
            val givenExpirationDate = currentTime + 2L
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeInterval)

            subject.updateSessionDuration(null, givenExpirationDate)

            advanceTimeBy(1200)

            subject.clearAndRemoveNotice()

            capturedEvent = null
            healthCheckTriggered = false

            subject.onMessage()

            // onMessage should not emit event since clearAndRemoveNotice already cleared the flag
            assertThat(capturedEvent).isNull()
            assertThat(healthCheckTriggered).isFalse()
        }

    @Test
    fun `when clearAndRemoveNotice then cancels pending timers`() =
        testScope.runTest {
            val givenNoticeInterval = 1L
            val givenExpirationDate = currentTime + 3L
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeInterval)

            subject.updateSessionDuration(null, givenExpirationDate)

            // Clear before timers fire
            subject.clearAndRemoveNotice()

            capturedEvent = null

            // Advance time past when timers would have fired
            advanceTimeBy(3000)

            // No events should be emitted since timers were cancelled
            assertThat(capturedEvent).isNull()
        }

    @Test
    fun `when notice time has already passed then emits SessionExpirationNotice immediately`() =
        testScope.runTest {
            val givenNoticeInterval = 60L // Notice interval is 60 seconds before expiration
            val givenExpirationDate = currentTime + 30L // Expiration in 30 seconds, notice time already passed
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeInterval)

            subject.updateSessionDuration(null, givenExpirationDate)

            // Event should be emitted immediately without waiting for timer
            val capturedExpirationNotice = capturedEvent as? Event.SessionExpirationNotice
            assertThat(capturedExpirationNotice).isEqualTo(Event.SessionExpirationNotice(givenExpirationDate - currentTime))
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
            val givenNoticeInterval = 1L
            val givenExpirationDate = currentTime + 2L
            val subject =
                SessionDurationHandler(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeInterval,
                    healthCheckPreNoticeTimeMillis = 300L,
                    eventHandler = countingEventHandler,
                    log = mockLog,
                    triggerHealthCheck = { healthCheckTriggered = true },
                    getCurrentTimestamp = { currentTime * 1000 },
                    dispatcher = testScope
                )

            subject.updateSessionDuration(null, givenExpirationDate)

            // Advance time for expiration notice to be sent
            advanceTimeBy(1200)

            subject.clearAndRemoveNotice()
            subject.clearAndRemoveNotice()
            subject.clearAndRemoveNotice()

            assertThat(eventCount).isEqualTo(1)
        }

    @Test
    fun `when onMessage then calculates updatedExpirationDate based on current time and duration`() =
        testScope.runTest {
            val givenDurationSeconds = 3600L
            val givenExpirationDate = currentTime + 10L
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = 1L)

            subject.updateSessionDuration(givenDurationSeconds, givenExpirationDate)
            capturedEvent = null

            subject.onMessage()

            // Advance time and check that health check timer reschedules instead of triggering
            advanceTimeBy(9100)

            // Health check should not be triggered because session was extended by onMessage
            assertThat(healthCheckTriggered).isFalse()
        }

    @Test
    fun `when health check timer fires and session was extended by onMessage then reschedules timers`() =
        testScope.runTest {
            val givenDurationSeconds = 100L
            val givenNoticeInterval = 1L
            val givenExpirationDate = currentTime + 3L
            val subject =
                createSubject(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeInterval,
                    healthCheckLeadTimeMillis = 500L
                )

            subject.updateSessionDuration(givenDurationSeconds, givenExpirationDate)

            // Simulate message activity before health check timer fires
            advanceTimeBy(1000)
            subject.onMessage()

            // Now let the health check timer fire
            advanceTimeBy(600)

            // Health check should NOT be triggered because session was extended
            assertThat(healthCheckTriggered).isFalse()
        }

    @Test
    fun `when health check timer fires and no message activity then triggers health check`() =
        testScope.runTest {
            val givenNoticeInterval = 1L
            val givenExpirationDate = currentTime + 3L
            val subject =
                createSubject(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeInterval,
                    healthCheckLeadTimeMillis = 500L
                )

            // Only set expiration, no duration - so onMessage won't calculate updated expiration
            subject.updateSessionDuration(null, givenExpirationDate)

            // Let health check timer fire without any message activity
            advanceTimeBy(1600)

            // Health check should be triggered
            assertThat(healthCheckTriggered).isTrue()
        }

    @Test
    fun `when expiration timer fires and session was extended by onMessage then reschedules timers`() =
        testScope.runTest {
            val givenDurationSeconds = 100L
            val givenNoticeInterval = 1L
            val givenExpirationDate = currentTime + 2L
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeInterval)

            subject.updateSessionDuration(givenDurationSeconds, givenExpirationDate)
            capturedEvent = null

            // Simulate message activity before expiration timer fires
            advanceTimeBy(500)
            subject.onMessage()

            // Now let the expiration timer fire
            advanceTimeBy(600)

            // Expiration notice should NOT be emitted because session was extended
            assertThat(capturedEvent).isNull()
        }

    @Test
    fun `when expiration timer fires and no message activity then emits expiration notice`() =
        testScope.runTest {
            val givenNoticeInterval = 1L
            val givenExpirationDate = currentTime + 2L
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeInterval)

            // Only set expiration, no duration - so onMessage won't calculate updated expiration
            subject.updateSessionDuration(null, givenExpirationDate)
            capturedEvent = null

            // Let expiration timer fire without any message activity
            advanceTimeBy(1100)

            // Expiration notice should be emitted
            assertThat(capturedEvent).isEqualTo(Event.SessionExpirationNotice(givenExpirationDate - currentTime))
        }

    @Test
    fun `when onMessage without durationSeconds set then does not calculate updatedExpirationDate`() =
        testScope.runTest {
            val givenNoticeInterval = 1L
            val givenExpirationDate = currentTime + 3L
            val subject =
                createSubject(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeInterval,
                    healthCheckLeadTimeMillis = 500L
                )

            // Set expiration without duration
            subject.updateSessionDuration(null, givenExpirationDate)

            // Call onMessage - should not set updatedExpirationDate since no duration
            subject.onMessage()

            // Let health check timer fire
            advanceTimeBy(1600)

            // Health check should be triggered since updatedExpirationDate was not set
            assertThat(healthCheckTriggered).isTrue()
        }

    @Test
    fun `when clear then resets updatedExpirationDate`() =
        testScope.runTest {
            val givenDurationSeconds = 100L
            val givenNoticeInterval = 1L
            val givenExpirationDate = currentTime + 3L
            val subject =
                createSubject(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeInterval,
                    healthCheckLeadTimeMillis = 500L
                )

            subject.updateSessionDuration(givenDurationSeconds, givenExpirationDate)
            subject.onMessage() // Sets updatedExpirationDate

            subject.clear()

            // Set new expiration after clear
            subject.updateSessionDuration(null, givenExpirationDate)

            // Let health check timer fire
            advanceTimeBy(1600)

            // Health check should be triggered since updatedExpirationDate was reset by clear
            assertThat(healthCheckTriggered).isTrue()
        }

    @Test
    fun `when expiration timer fires and updatedExpirationDate is greater than currentExpirationDate then reschedules with updatedExpirationDate`() =
        testScope.runTest {
            val givenDurationSeconds = 100L
            val givenNoticeInterval = 1L
            val givenExpirationDate = currentTime + 2L
            val subject =
                createSubject(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeInterval,
                    healthCheckLeadTimeMillis = 0L // Disable health check timer
                )

            subject.updateSessionDuration(givenDurationSeconds, givenExpirationDate)

            // Call onMessage to set updatedExpirationDate before any timer fires
            subject.onMessage()

            capturedEvent = null

            // Advance time to let the expiration timer fire
            advanceTimeBy(1100)

            // Expiration notice should NOT be emitted because shouldReschedule() returned true
            assertThat(capturedEvent).isNull()
        }

    @Test
    fun `when expiration notice is emitted then schedules health check at expiration time`() =
        testScope.runTest {
            val givenNoticeInterval = 1L
            val givenExpirationDate = currentTime + 2L
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeInterval)

            subject.updateSessionDuration(null, givenExpirationDate)

            // Advance time for expiration notice to be sent
            advanceTimeBy(1100)

            assertThat(capturedEvent).isEqualTo(Event.SessionExpirationNotice(givenExpirationDate - currentTime))
            healthCheckTriggered = false

            // Advance time for health check at expiration time (remaining time to expiration is ~1 second)
            advanceTimeBy(1100)

            assertThat(healthCheckTriggered).isTrue()
        }

    @Test
    fun `when expiration notice is emitted with zero time to expiration then triggers health check immediately`() =
        testScope.runTest {
            val givenNoticeInterval = 60L // Notice interval is 60 seconds before expiration
            val givenExpirationDate = currentTime + 30L // Expiration in 30 seconds, notice time already passed
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeInterval)

            subject.updateSessionDuration(null, givenExpirationDate)

            // Event should be emitted immediately (notice time already passed)
            assertThat(capturedEvent).isEqualTo(Event.SessionExpirationNotice(givenExpirationDate - currentTime))
            // Health check should be scheduled for the remaining 30 seconds
            healthCheckTriggered = false

            // Advance time to expiration
            advanceTimeBy(30100)

            assertThat(healthCheckTriggered).isTrue()
        }

    @Test
    fun `when clear then cancels expiration health check timer`() =
        testScope.runTest {
            val givenNoticeInterval = 1L
            val givenExpirationDate = currentTime + 2L
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeInterval)

            subject.updateSessionDuration(null, givenExpirationDate)

            // Advance time for expiration notice to be sent
            advanceTimeBy(1100)

            assertThat(capturedEvent).isEqualTo(Event.SessionExpirationNotice(givenExpirationDate - currentTime))
            healthCheckTriggered = false

            subject.clear()

            // Advance time past when health check would have fired
            advanceTimeBy(2000)

            assertThat(healthCheckTriggered).isFalse()
        }

    @Test
    fun `when onMessage after expiration notice then cancels expiration health check timer`() =
        testScope.runTest {
            var healthCheckCount = 0
            val givenNoticeInterval = 1L
            val givenExpirationDate = currentTime + 2L
            val subject =
                SessionDurationHandler(
                    sessionExpirationNoticeIntervalSeconds = givenNoticeInterval,
                    healthCheckPreNoticeTimeMillis = 300L,
                    eventHandler = mockEventHandler,
                    log = mockLog,
                    triggerHealthCheck = { healthCheckCount++ },
                    getCurrentTimestamp = { currentTime * 1000 },
                    dispatcher = testScope
                )

            subject.updateSessionDuration(3600L, givenExpirationDate)

            // Advance time for expiration notice to be sent
            advanceTimeBy(1100)

            assertThat(capturedEvent).isEqualTo(Event.SessionExpirationNotice(givenExpirationDate - currentTime))

            // Record health check count after notice (should have one from pre-notice timer)
            val healthCheckCountAfterNotice = healthCheckCount

            // Send message which should cancel expiration health check timer and trigger immediate health check
            subject.onMessage()

            val expectedHealthCheckCount = healthCheckCountAfterNotice + 1
            assertThat(healthCheckCount).isEqualTo(expectedHealthCheckCount)

            // Advance time past when expiration health check would have fired
            advanceTimeBy(2000)

            // No additional health check should have been triggered since timer was cancelled
            assertThat(healthCheckCount).isEqualTo(expectedHealthCheckCount)
        }

    @Test
    fun `when clearAndRemoveNotice then cancels expiration health check timer`() =
        testScope.runTest {
            val givenNoticeInterval = 1L
            val givenExpirationDate = currentTime + 2L
            val subject = createSubject(sessionExpirationNoticeIntervalSeconds = givenNoticeInterval)

            subject.updateSessionDuration(null, givenExpirationDate)

            // Advance time for expiration notice to be sent
            advanceTimeBy(1100)

            assertThat(capturedEvent).isEqualTo(Event.SessionExpirationNotice(givenExpirationDate - currentTime))
            healthCheckTriggered = false

            subject.clearAndRemoveNotice()

            // Advance time past when health check would have fired
            advanceTimeBy(2000)

            assertThat(healthCheckTriggered).isFalse()
        }
}
