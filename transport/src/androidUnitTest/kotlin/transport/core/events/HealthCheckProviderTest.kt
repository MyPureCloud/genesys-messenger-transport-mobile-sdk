package transport.core.events

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.genesys.cloud.messenger.transport.core.events.HEALTH_CHECK_COOL_DOWN_MILLISECONDS
import com.genesys.cloud.messenger.transport.core.events.HealthCheckProvider
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogTag
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import transport.util.Request
import kotlin.test.BeforeTest
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HealthCheckProviderTest {
    private val mockLogger: Log = mockk(relaxed = true)
    private val logSlot = mutableListOf<() -> String>()
    private val mockTimestampFunction: () -> Long = spyk()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private var healthCheckTriggered = false
    private lateinit var subject: HealthCheckProvider

    @BeforeTest
    fun setUp() {
        healthCheckTriggered = false
        logSlot.clear()
        clearMocks(mockLogger, mockTimestampFunction)
        every { mockTimestampFunction.invoke() } answers { Platform().epochMillis() }
        subject = HealthCheckProvider(
            log = mockLogger,
            getCurrentTimestamp = mockTimestampFunction,
            triggerHealthCheck = { healthCheckTriggered = true },
            dispatcher = testScope
        )
    }

    @Test
    fun `when encodeRequest()`() {
        val result = subject.encodeRequest(token = Request.token)

        assertThat(result).isNotNull()
        assertTrue(Request.isEchoRequest(result!!))
    }

    @Test
    fun `when encodeRequest() with cool down`() {
        val healthCheckCoolDownInMilliseconds = HEALTH_CHECK_COOL_DOWN_MILLISECONDS + 250
        val firstResult = subject.encodeRequest(token = Request.token)
        every { mockTimestampFunction.invoke() } answers { Platform().epochMillis() + healthCheckCoolDownInMilliseconds }
        val secondResult = subject.encodeRequest(token = Request.token)

        verifySequence {
            mockTimestampFunction.invoke()
            mockTimestampFunction.invoke()
        }
        assertThat(firstResult).isNotNull()
        assertTrue(Request.isEchoRequest(firstResult!!))
        assertThat(secondResult).isNotNull()
        assertTrue(Request.isEchoRequest(secondResult!!))
    }

    @Test
    fun `when encodeRequest() during cooldown then returns null and schedules deferred health check`() {
        val firstResult = subject.encodeRequest(token = Request.token)
        val secondResult = subject.encodeRequest(token = Request.token)

        verify {
            mockLogger.i(capture(logSlot))
        }
        assertThat(firstResult).isNotNull()
        assertTrue(Request.isEchoRequest(firstResult!!))
        assertThat(secondResult).isNull()
        val capturedLogMessage = logSlot.find { it.invoke().contains("deferred") }
        assertThat(capturedLogMessage).isNotNull()
    }

    @Test
    fun `when encodeRequest() without cool down but with clear`() {
        val firstResult = subject.encodeRequest(token = Request.token)
        subject.clear()
        val secondResult = subject.encodeRequest(token = Request.token)

        assertThat(firstResult).isNotNull()
        assertTrue(Request.isEchoRequest(firstResult!!))
        assertThat(secondResult).isNotNull()
        assertTrue(Request.isEchoRequest(secondResult!!))
    }

    @Test
    fun `when getCurrentTimestamp()`() {
        val givenAcceptableRangeOffset = 10

        val result = System.currentTimeMillis()

        assertTrue { subject.getCurrentTimestamp() in (result - givenAcceptableRangeOffset)..(result + givenAcceptableRangeOffset) }
    }

    @Test
    fun `validate default constructor`() {
        val givenAcceptableRangeOffset = 10
        val currentTime = System.currentTimeMillis()

        val defaultSubject = HealthCheckProvider()

        assertThat(defaultSubject.log.logger.tag).isEqualTo(LogTag.HEALTH_CHECK_PROVIDER)
        assertTrue { defaultSubject.getCurrentTimestamp() in (currentTime - givenAcceptableRangeOffset)..(currentTime + givenAcceptableRangeOffset) }
    }

    @Test
    fun `when deferred health check timer fires then triggers callback`() = testScope.runTest {
        val baseTime = Platform().epochMillis()
        var currentTime = baseTime
        every { mockTimestampFunction.invoke() } answers { currentTime }

        subject.encodeRequest(token = Request.token)
        assertFalse(healthCheckTriggered)

        currentTime = baseTime + 10000
        subject.encodeRequest(token = Request.token)

        assertFalse(healthCheckTriggered)

        advanceTimeBy(HEALTH_CHECK_COOL_DOWN_MILLISECONDS)

        assertTrue(healthCheckTriggered)
    }

    @Test
    fun `when clear called then cancels deferred health check timer`() = testScope.runTest {
        val baseTime = Platform().epochMillis()
        var currentTime = baseTime
        every { mockTimestampFunction.invoke() } answers { currentTime }

        subject.encodeRequest(token = Request.token)

        currentTime = baseTime + 10000
        subject.encodeRequest(token = Request.token)

        subject.clear()

        advanceTimeBy(HEALTH_CHECK_COOL_DOWN_MILLISECONDS)

        assertFalse(healthCheckTriggered)
    }

    @Test
    fun `when encodeRequest() called multiple times during cooldown then only one deferred timer is active`() = testScope.runTest {
        var triggerCount = 0
        val baseTime = Platform().epochMillis()
        var currentTime = baseTime
        every { mockTimestampFunction.invoke() } answers { currentTime }

        val customSubject = HealthCheckProvider(
            log = mockLogger,
            getCurrentTimestamp = mockTimestampFunction,
            triggerHealthCheck = { triggerCount++ },
            dispatcher = testScope
        )

        customSubject.encodeRequest(token = Request.token)

        currentTime = baseTime + 5000
        customSubject.encodeRequest(token = Request.token)

        currentTime = baseTime + 10000
        customSubject.encodeRequest(token = Request.token)

        currentTime = baseTime + 15000
        customSubject.encodeRequest(token = Request.token)

        advanceTimeBy(HEALTH_CHECK_COOL_DOWN_MILLISECONDS)

        assertThat(triggerCount).isEqualTo(1)
    }

    @Test
    fun `when setTriggerHealthCheck then uses new callback for deferred health check`() = testScope.runTest {
        var newCallbackTriggered = false
        val baseTime = Platform().epochMillis()
        var currentTime = baseTime
        every { mockTimestampFunction.invoke() } answers { currentTime }

        subject.encodeRequest(token = Request.token)

        subject.setTriggerHealthCheck { newCallbackTriggered = true }

        currentTime = baseTime + 10000
        subject.encodeRequest(token = Request.token)

        advanceTimeBy(HEALTH_CHECK_COOL_DOWN_MILLISECONDS)

        assertFalse(healthCheckTriggered)
        assertTrue(newCallbackTriggered)
    }

    @Test
    fun `when deferred timer fires and cooldown has passed then health check is triggered`() = testScope.runTest {
        val baseTime = Platform().epochMillis()
        var currentTime = baseTime
        every { mockTimestampFunction.invoke() } answers { currentTime }

        subject.encodeRequest(token = Request.token)

        currentTime = baseTime + 20000
        val remainingCooldown = HEALTH_CHECK_COOL_DOWN_MILLISECONDS - 20000
        subject.encodeRequest(token = Request.token)

        advanceTimeBy(remainingCooldown + 100)

        assertTrue(healthCheckTriggered)
    }
}
