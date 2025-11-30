package transport.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.util.ActionTimer
import com.genesys.cloud.messenger.transport.util.logs.Log
import io.mockk.MockKAnnotations
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class ActionTimerTest {
    private val mockLogger: Log = mockk(relaxed = true)
    private val logSlot = mutableListOf<() -> String>()
    private var actionExecuted = false
    private val givenAction: () -> Unit = { actionExecuted = true }
    private val dispatcher: CoroutineDispatcher = Dispatchers.Unconfined
    private val givenDispatcher = CoroutineScope(dispatcher + SupervisorJob())

    private lateinit var subject: ActionTimer

    @ExperimentalCoroutinesApi
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(dispatcher)
        actionExecuted = false
        subject =
            ActionTimer(
                log = mockLogger,
                action = givenAction,
                dispatcher = givenDispatcher,
            )
    }

    @ExperimentalCoroutinesApi
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when start() executes action after delay`() {
        runBlocking {
            subject.start(delayMillis = 100)
            delay(150)
        }

        assertThat(actionExecuted).isTrue()
        verify { mockLogger.i(capture(logSlot)) }
        assertThat(logSlot[0].invoke()).isEqualTo("Starting timer with delay: 100 ms")
        assertThat(logSlot[1].invoke()).isEqualTo("Timer expired, executing action")
    }

    @Test
    fun `when start() timer is active`() {
        subject.start(delayMillis = 5000)

        assertThat(subject.isActive()).isTrue()
    }

    @Test
    fun `when cancel() before timer expires`() {
        runBlocking {
            subject.start(delayMillis = 1000)
            delay(50)
            subject.cancel()
            delay(1000)
        }

        assertThat(actionExecuted).isFalse()
        verify { mockLogger.i(capture(logSlot)) }
        assertThat(logSlot[0].invoke()).isEqualTo("Starting timer with delay: 1000 ms")
        assertThat(logSlot[1].invoke()).isEqualTo("Cancelling timer")
    }

    @Test
    fun `when cancel() timer is not active`() {
        runBlocking {
            subject.start(delayMillis = 1000)
            subject.cancel()
        }

        assertThat(subject.isActive()).isFalse()
    }

    @Test
    fun `when start() is called multiple times cancels previous timer`() {
        var executionCount = 0
        val countingAction: () -> Unit = { executionCount++ }
        subject =
            ActionTimer(
                log = mockLogger,
                action = countingAction,
                dispatcher = givenDispatcher,
            )

        runBlocking {
            subject.start(delayMillis = 500)
            delay(100)
            subject.start(delayMillis = 500)
            delay(100)
            subject.start(delayMillis = 100)
            delay(150)
        }

        assertThat(executionCount).isEqualTo(1)
    }

    @Test
    fun `when cancel() on inactive timer does nothing`() {
        subject.cancel()

        assertThat(subject.isActive()).isFalse()
        verify(exactly = 0) { mockLogger.i(any()) }
    }

    @Test
    fun `when isActive() returns false after timer expires`() {
        runBlocking {
            subject.start(delayMillis = 100)
            delay(150)
        }

        assertThat(subject.isActive()).isFalse()
        assertThat(actionExecuted).isTrue()
    }
}
