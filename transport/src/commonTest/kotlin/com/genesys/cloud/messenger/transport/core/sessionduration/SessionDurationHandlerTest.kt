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

    private lateinit var subject: SessionDurationHandler

    @BeforeTest
    fun setUp() {
        capturedEvent = null
        currentTime = 1000000000L
        healthCheckTriggered = false
        subject =
            SessionDurationHandler(
                sessionExpirationNoticeInterval = 60L,
                healthCheckPreNoticeTimeMillis = 500L,
                eventHandler = mockEventHandler,
                log = mockLog,
                triggerHealthCheck = { healthCheckTriggered = true },
                getCurrentTimestamp = { currentTime * 1000 }
            )
    }

    private fun createSubject(
        sessionExpirationNoticeInterval: Long = 60L,
        healthCheckLeadTimeMillis: Long = 500L,
        getCurrentTime: () -> Long = { currentTime * 1000 },
        onHealthCheckTriggered: () -> Unit = { healthCheckTriggered = true }
    ) = SessionDurationHandler(
        sessionExpirationNoticeInterval = sessionExpirationNoticeInterval,
        healthCheckPreNoticeTimeMillis = healthCheckLeadTimeMillis,
        eventHandler = mockEventHandler,
        log = mockLog,
        triggerHealthCheck = onHealthCheckTriggered,
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
    fun `when updateSessionDuration with new expirationDate then schedules timer`() =
        runBlocking {
            val givenNoticeInterval = 60L // 60 seconds
            val givenExpirationDate = 1000000300L // Current time + 300 seconds
            val subject = createSubject(sessionExpirationNoticeInterval = givenNoticeInterval)

            subject.updateSessionDuration(null, givenExpirationDate)

            // Timer should be scheduled but not expired yet
            assertThat(capturedEvent).isNull()
        }

    @Test
    fun `when timer expires then emits SessionExpirationNotice event with time to expiration`() =
        runBlocking {
            withTimeout(DEFAULT_TIMEOUT) {
                val givenNoticeInterval = 1L
                val givenExpirationDate = currentTime + 2L
                val expectedTimeToExpiration = givenExpirationDate - currentTime
                val subject = createSubject(sessionExpirationNoticeInterval = givenNoticeInterval)

                subject.updateSessionDuration(null, givenExpirationDate)

                delay(1200)

                val capturedExpirationNotice = capturedEvent as? Event.SessionExpirationNotice
                assertThat(capturedExpirationNotice).isEqualTo(Event.SessionExpirationNotice(expectedTimeToExpiration))
            }
        }

    @Test
    fun `when clear then resets state and cancels timer`() =
        runBlocking {
            withTimeout(DEFAULT_TIMEOUT) {
                val givenNoticeInterval = 1L
                val givenExpirationDate = currentTime + 2L
                val subject = createSubject(sessionExpirationNoticeInterval = givenNoticeInterval)

                subject.updateSessionDuration(3600L, givenExpirationDate)
                capturedEvent = null

                subject.clear()

                // Wait to ensure timer would have fired if not cancelled
                delay(1500)

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
    fun `when updateSessionDuration multiple times with different expirationDates then only last timer is active`() =
        runBlocking {
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

    @Test
    fun `when health check timer fires then triggers health check callback`() =
        runBlocking {
            withTimeout(DEFAULT_TIMEOUT) {
                val givenNoticeInterval = 1L
                val givenHealthCheckLeadTime = 500L
                val givenExpirationDate = currentTime + 3L
                val subject =
                    createSubject(
                        sessionExpirationNoticeInterval = givenNoticeInterval,
                        healthCheckLeadTimeMillis = givenHealthCheckLeadTime
                    )

                subject.updateSessionDuration(null, givenExpirationDate)

                // Wait for health check timer to fire (should fire 500ms before expiration notice)
                delay(1600)

                assertThat(healthCheckTriggered).isEqualTo(true)
            }
        }

    @Test
    fun `when health check lead time is too short then triggers health check immediately`() =
        runBlocking {
            val givenNoticeInterval = 1L
            val givenHealthCheckLeadTime = 5000L // 5 seconds lead time
            val givenExpirationDate = currentTime + 2L // Only 1 second until notice time
            val subject =
                createSubject(
                    sessionExpirationNoticeInterval = givenNoticeInterval,
                    healthCheckLeadTimeMillis = givenHealthCheckLeadTime
                )

            subject.updateSessionDuration(null, givenExpirationDate)

            // Health check should be triggered immediately since lead time is too short
            assertThat(healthCheckTriggered).isEqualTo(true)
        }

    @Test
    fun `when updateSessionDuration with new expirationDate then reschedules both timers`() =
        runBlocking {
            withTimeout(DEFAULT_TIMEOUT) {
                val givenNoticeInterval = 1L
                val givenHealthCheckLeadTime = 300L
                healthCheckTriggered = false
                val subject =
                    createSubject(
                        sessionExpirationNoticeInterval = givenNoticeInterval,
                        healthCheckLeadTimeMillis = givenHealthCheckLeadTime
                    )

                // Set initial expiration date
                subject.updateSessionDuration(null, currentTime + 2L)

                // Update to a later expiration date before timers fire
                subject.updateSessionDuration(null, currentTime + 10L)

                // Wait for when first timers would have expired
                delay(1200)

                // Neither timer should have fired yet since they were rescheduled
                assertThat(healthCheckTriggered).isEqualTo(false)
                assertThat(capturedEvent).isNull()
            }
        }

    @Test
    fun `when clear then cancels health check timer`() =
        runBlocking {
            withTimeout(DEFAULT_TIMEOUT) {
                val givenNoticeInterval = 1L
                val givenHealthCheckLeadTime = 300L
                val givenExpirationDate = currentTime + 2L
                val subject =
                    createSubject(
                        sessionExpirationNoticeInterval = givenNoticeInterval,
                        healthCheckLeadTimeMillis = givenHealthCheckLeadTime
                    )

                subject.updateSessionDuration(null, givenExpirationDate)

                subject.clear()

                // Wait for when health check timer would have fired
                delay(1000)

                assertThat(healthCheckTriggered).isEqualTo(false)
            }
        }

    @Test
    fun `when triggerHealthCheck is set after construction then timer uses updated callback`() =
        runBlocking {
            withTimeout(DEFAULT_TIMEOUT) {
                var customCallbackTriggered = false
                val givenNoticeInterval = 1L
                val givenHealthCheckLeadTime = 500L
                val givenExpirationDate = currentTime + 3L
                val subject =
                    SessionDurationHandler(
                        sessionExpirationNoticeInterval = givenNoticeInterval,
                        healthCheckPreNoticeTimeMillis = givenHealthCheckLeadTime,
                        eventHandler = mockEventHandler,
                        log = mockLog,
                        getCurrentTimestamp = { currentTime * 1000 }
                    )

                subject.setTriggerHealthCheck { customCallbackTriggered = true }

                subject.updateSessionDuration(null, givenExpirationDate)

                // Wait for health check timer to fire
                delay(1600)

                assertThat(customCallbackTriggered).isEqualTo(true)
            }
        }

    @Test
    fun `when triggerHealthCheck is reassigned then new callback is used`() =
        runBlocking {
            withTimeout(DEFAULT_TIMEOUT) {
                var firstCallbackTriggered = false
                var secondCallbackTriggered = false
                val givenNoticeInterval = 1L
                val givenHealthCheckLeadTime = 500L
                val subject =
                    SessionDurationHandler(
                        sessionExpirationNoticeInterval = givenNoticeInterval,
                        healthCheckPreNoticeTimeMillis = givenHealthCheckLeadTime,
                        eventHandler = mockEventHandler,
                        log = mockLog,
                        getCurrentTimestamp = { currentTime * 1000 }
                    )

                subject.setTriggerHealthCheck { firstCallbackTriggered = true }
                // Reassign to a different callback
                subject.setTriggerHealthCheck { secondCallbackTriggered = true }

                val givenExpirationDate = currentTime + 3L
                subject.updateSessionDuration(null, givenExpirationDate)

                // Wait for health check timer to fire
                delay(1600)

                assertThat(firstCallbackTriggered).isEqualTo(false)
                assertThat(secondCallbackTriggered).isEqualTo(true)
            }
        }

    @Test
    fun `when same expirationDate is provided twice then timer is not rescheduled`() =
        runBlocking {
            withTimeout(DEFAULT_TIMEOUT) {
                var healthCheckCount = 0
                val givenNoticeInterval = 1L
                val givenHealthCheckLeadTime = 300L
                val givenExpirationDate = currentTime + 2L
                val subject =
                    SessionDurationHandler(
                        sessionExpirationNoticeInterval = givenNoticeInterval,
                        healthCheckPreNoticeTimeMillis = givenHealthCheckLeadTime,
                        eventHandler = mockEventHandler,
                        log = mockLog,
                        triggerHealthCheck = { healthCheckCount++ },
                        getCurrentTimestamp = { currentTime * 1000 }
                    )

                subject.updateSessionDuration(null, givenExpirationDate)
                subject.updateSessionDuration(null, givenExpirationDate)

                // Wait for timer to fire
                delay(1000)

                assertThat(healthCheckCount).isEqualTo(1)
            }
        }
}
