package transport.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.util.ActionTimer
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import io.mockk.MockKAnnotations
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActionTimerTest {
    private val mockLogger: Log = mockk(relaxed = true)
    private val logSlot = mutableListOf<() -> String>()
    private var actionExecuted = false
    private val givenAction: () -> Unit = { actionExecuted = true }
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var subject: ActionTimer

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        actionExecuted = false
        subject =
            ActionTimer(
                log = mockLogger,
                action = givenAction,
                dispatcher = testScope,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when start() executes action after delay`() =
        testScope.runTest {
            subject.start(delayMillis = 100)
            advanceTimeBy(150)

            assertThat(actionExecuted).isTrue()
            verify { mockLogger.i(capture(logSlot)) }
            assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.startingTimer(100))
            assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.TIMER_EXPIRED_EXECUTING_ACTION)
        }

    @Test
    fun `when start() timer is active`() =
        testScope.runTest {
            subject.start(delayMillis = 5000)

            assertThat(subject.isActive()).isTrue()
        }

    @Test
    fun `when cancel() before timer expires`() =
        testScope.runTest {
            subject.start(delayMillis = 1000)
            advanceTimeBy(50)
            subject.cancel()
            advanceTimeBy(1000)

            assertThat(actionExecuted).isFalse()
            verify { mockLogger.i(capture(logSlot)) }
            assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.startingTimer(1000))
            assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.CANCELLING_TIMER)
        }

    @Test
    fun `when cancel() timer is not active`() =
        testScope.runTest {
            subject.start(delayMillis = 1000)
            subject.cancel()

            assertThat(subject.isActive()).isFalse()
        }

    @Test
    fun `when start() is called multiple times cancels previous timer`() =
        testScope.runTest {
            var executionCount = 0
            val countingAction: () -> Unit = { executionCount++ }
            subject =
                ActionTimer(
                    log = mockLogger,
                    action = countingAction,
                    dispatcher = testScope,
                )

            subject.start(delayMillis = 500)
            advanceTimeBy(100)
            subject.start(delayMillis = 500)
            advanceTimeBy(100)
            subject.start(delayMillis = 100)
            advanceTimeBy(150)

            assertThat(executionCount).isEqualTo(1)
        }

    @Test
    fun `when cancel() on inactive timer does nothing`() =
        testScope.runTest {
            subject.cancel()

            assertThat(subject.isActive()).isFalse()
            verify(exactly = 0) { mockLogger.i(any()) }
        }

    @Test
    fun `when isActive() returns false after timer expires`() =
        testScope.runTest {
            subject.start(delayMillis = 100)
            advanceTimeBy(150)

            assertThat(subject.isActive()).isFalse()
            assertThat(actionExecuted).isTrue()
        }
}
