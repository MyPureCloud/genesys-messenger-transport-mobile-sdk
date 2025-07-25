package transport.core

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.MessagingClient.State
import com.genesys.cloud.messenger.transport.core.StateChange
import com.genesys.cloud.messenger.transport.core.StateMachineImpl
import com.genesys.cloud.messenger.transport.core.isClosed
import com.genesys.cloud.messenger.transport.core.isClosing
import com.genesys.cloud.messenger.transport.core.isConfigured
import com.genesys.cloud.messenger.transport.core.isConnected
import com.genesys.cloud.messenger.transport.core.isConnecting
import com.genesys.cloud.messenger.transport.core.isError
import com.genesys.cloud.messenger.transport.core.isIdle
import com.genesys.cloud.messenger.transport.core.isInactive
import com.genesys.cloud.messenger.transport.core.isReadOnly
import com.genesys.cloud.messenger.transport.core.isReconnecting
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.util.logs.LogTag
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StateMachineTest {
    internal val mockLogger: Log = mockk(relaxed = true)
    internal val logSlot = mutableListOf<() -> String>()

    private val subject = StateMachineImpl(mockLogger)
    private val mockStateListener: (State) -> Unit = spyk()
    private val mockStateChangedListener: (StateChange) -> Unit = spyk()

    @Before
    fun setup() {
        subject.stateListener = mockStateListener
        subject.stateChangedListener = mockStateChangedListener
    }

    @Test
    fun whenSubjectWasInitialized() {
        assertThat(subject.currentState).isIdle()
        assertTrue { subject.isInactive() }
    }

    @Test
    fun whenOnConnectionOpened() {
        val expectedStateChange = StateChange(State.Idle, State.Connected)

        subject.onConnectionOpened()

        assertThat(subject.currentState).isConnected()
        assertFalse { subject.isInactive() }
        verifySequence {
            mockLogger.i(capture(logSlot))
            mockStateListener(State.Connected)
            mockStateChangedListener(expectedStateChange)
        }
        assertThat(logSlot[0].invoke()).isEqualTo(
            LogMessages.stateChanged(
                expectedStateChange.newState,
                expectedStateChange.newState
            )
        )
    }

    @Test
    fun whenOnConnectionOpenedAfterOnReconnect() {
        val expectedStateChange = StateChange(State.Idle, State.Reconnecting)

        subject.onReconnect()
        subject.onConnectionOpened()

        assertThat(subject.currentState).isReconnecting()
        assertFalse { subject.isInactive() }
        verify {
            mockStateListener(State.Reconnecting)
            mockStateChangedListener(expectedStateChange)
        }
    }

    @Test
    fun whenOnConnect() {
        val expectedStateChange = StateChange(State.Idle, State.Connecting)

        subject.onConnect()

        assertThat(subject.currentState).isConnecting()
        assertFalse { subject.isInactive() }
        verify {
            mockStateListener(State.Connecting)
            mockStateChangedListener(expectedStateChange)
        }
    }

    @Test
    fun whenOnConnectAfterOnReconnect() {
        val expectedStateChange = StateChange(State.Idle, State.Reconnecting)

        subject.onReconnect()
        subject.onConnect()

        assertThat(subject.currentState).isReconnecting()
        assertFalse { subject.isInactive() }
        verify {
            mockStateListener(State.Reconnecting)
            mockStateChangedListener(expectedStateChange)
        }
    }

    @Test
    fun whenOnConnectAndCurrentStateIsConnected() {
        subject.onConnectionOpened()

        assertFailsWith<IllegalStateException> { subject.onConnect() }
    }

    @Test
    fun whenOnConnectAndCurrentStateIsConfigured() {
        subject.onSessionConfigured(connected = true, newSession = true)

        assertFailsWith<IllegalStateException> { subject.onConnect() }
    }

    @Test
    fun whenOnConnectAndCurrentStateIsReadOnly() {
        subject.onReadOnly()

        assertFailsWith<IllegalStateException> { subject.onConnect() }
    }

    @Test
    fun whenOnReconnect() {
        val expectedStateChange = StateChange(State.Idle, State.Reconnecting)

        subject.onReconnect()

        assertThat(subject.currentState).isReconnecting()
        assertFalse { subject.isInactive() }
        verify {
            mockStateListener(State.Reconnecting)
            mockStateChangedListener(expectedStateChange)
        }
    }

    @Test
    fun whenOnSessionConfigured() {
        val expectedStateChange =
            StateChange(State.Idle, State.Configured(connected = true, newSession = true))

        subject.onSessionConfigured(connected = true, newSession = true)

        assertThat(subject.currentState).isConfigured(
            connected = true,
            newSession = true,
        )
        assertFalse { subject.isInactive() }
        verify {
            mockStateListener(State.Configured(connected = true, newSession = true))
            mockStateChangedListener(expectedStateChange)
        }
    }

    @Test
    fun whenOnSessionConfiguredAfterOnReconnect() {
        val expectedStateChange =
            StateChange(State.Reconnecting, State.Configured(connected = true, newSession = true))

        subject.onReconnect()
        subject.onSessionConfigured(connected = true, newSession = true)

        assertThat(subject.currentState).isConfigured(
            connected = true,
            newSession = true,
        )
        assertFalse { subject.isInactive() }
        verify {
            mockStateListener(State.Configured(connected = true, newSession = true))
            mockStateChangedListener(expectedStateChange)
        }
    }

    @Test
    fun whenOnClosingAfterConnectionWasOpened() {
        val expectedStateChange =
            StateChange(State.Connected, State.Closing(code = 1, reason = "A reason."))

        subject.onConnectionOpened()
        subject.onClosing(code = 1, reason = "A reason.")

        assertThat(subject.currentState).isClosing(code = 1, reason = "A reason.")
        assertTrue { subject.isInactive() }
        verify {
            mockStateListener(State.Closing(code = 1, reason = "A reason."))
            mockStateChangedListener(expectedStateChange)
        }
    }

    @Test
    fun whenOnClosingAndCurrentStateIsIdle() {
        assertFailsWith<IllegalStateException> { subject.onClosing(code = 1, reason = "A reason.") }
    }

    @Test
    fun whenOnClosingAndCurrentStateIsClosed() {
        subject.onClosed(code = 1, reason = "A reason.")

        assertFailsWith<IllegalStateException> { subject.onClosing(code = 1, reason = "A reason.") }
    }

    @Test
    fun whenOnClosingWhenStateIsError() {
        subject.onError(code = ErrorCode.WebsocketError, message = "A message.")

        assertFailsWith<IllegalStateException> { subject.onClosing(code = 1, reason = "A reason.") }
    }

    @Test
    fun whenOnClosed() {
        val expectedStateChange =
            StateChange(State.Idle, State.Closed(code = 1, reason = "A reason."))

        subject.onClosed(code = 1, reason = "A reason.")

        assertThat(subject.currentState).isClosed(code = 1, reason = "A reason.")
        assertTrue { subject.isInactive() }
        verify {
            mockStateListener(State.Closed(code = 1, reason = "A reason."))
            mockStateChangedListener(expectedStateChange)
        }
    }

    @Test
    fun whenOnError() {
        val expectedStateChange =
            StateChange(State.Idle, State.Error(ErrorCode.WebsocketError, "A message."))

        subject.onError(code = ErrorCode.WebsocketError, message = "A message.")

        assertThat(subject.currentState).isError(
            code = ErrorCode.WebsocketError,
            message = "A message."
        )
        assertTrue { subject.isInactive() }
        verify {
            mockStateListener(
                State.Error(
                    code = ErrorCode.WebsocketError,
                    message = "A message."
                )
            )
            mockStateChangedListener(expectedStateChange)
        }
    }

    @Test
    fun whenOnReadOnly() {
        val expectedStateChange = StateChange(State.Idle, State.ReadOnly)

        subject.onReadOnly()

        assertThat(subject.currentState).isReadOnly()
        verify {
            mockStateListener(State.ReadOnly)
            mockStateChangedListener(expectedStateChange)
        }
    }

    @Test
    fun whenOnReadOnlyAfterOnReconnect() {
        val expectedStateChange = StateChange(State.Reconnecting, State.ReadOnly)

        subject.onReconnect()
        subject.onReadOnly()

        assertThat(subject.currentState).isReadOnly()
        verify {
            mockStateListener(State.ReadOnly)
            mockStateChangedListener(expectedStateChange)
        }
    }

    @Test
    fun verifyFullStateTransitionCycle() {
        subject.onConnect()
        assertThat(subject.currentState).isConnecting()
        subject.onConnectionOpened()
        assertThat(subject.currentState).isConnected()
        subject.onSessionConfigured(connected = true, newSession = true)
        assertThat(subject.currentState).isConfigured(connected = true, newSession = true)
        subject.onReconnect()
        assertThat(subject.currentState).isReconnecting()
        subject.onConnect()
        assertThat(subject.currentState).isReconnecting()
        subject.onConnectionOpened()
        assertThat(subject.currentState).isReconnecting()
        subject.onSessionConfigured(connected = true, newSession = false)
        assertThat(subject.currentState).isConfigured(connected = true, newSession = false)
        subject.onReadOnly()
        assertThat(subject.currentState).isReadOnly()
        subject.onClosing(100, "sss")
        assertThat(subject.currentState).isClosing(100, "sss")
        subject.onClosed(100, "sss")
        assertThat(subject.currentState).isClosed(100, "sss")
    }

    @org.junit.Test
    fun `validate default constructor`() {
        val subject = StateMachineImpl()

        assertThat(subject.log.logger.tag).isEqualTo(LogTag.STATE_MACHINE)
    }
}
