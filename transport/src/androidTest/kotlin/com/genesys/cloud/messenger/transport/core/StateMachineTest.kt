package com.genesys.cloud.messenger.transport.core

import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import com.genesys.cloud.messenger.transport.core.MessagingClient.State
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertFailsWith

class StateMachineTest {
    private val subject = StateMachineImpl()
    private val mockStateListener: (State) -> Unit = spyk()
    private val mockOnStateChanged: (StateChange) -> Unit = spyk()

    @Before
    fun setup() {
        subject.stateListener = mockStateListener
        subject.onStateChanged = mockOnStateChanged
    }

    @Test
    fun whenSubjectWasInitialized() {
        assertThat(subject).isIdle()
    }

    @Test
    fun whenOnConnectionOpened() {
        val expectedStateChange = StateChange(State.Idle, State.Connected)

        subject.onConnectionOpened()

        assertThat(subject).isConnected()
        verify { mockStateListener(State.Connected) }
        verify { mockOnStateChanged(expectedStateChange) }
    }

    @Test
    fun whenOnConnectionOpenedAfterOnReconnect() {
        val expectedStateChange = StateChange(State.Idle, State.Reconnecting)

        subject.onReconnect()
        subject.onConnectionOpened()

        assertThat(subject).isReconnecting()
        verify { mockStateListener(State.Reconnecting) }
        verify { mockOnStateChanged(expectedStateChange) }
    }

    @Test
    fun whenOnConnect() {
        val expectedStateChange = StateChange(State.Idle, State.Connecting)

        subject.onConnect()

        assertThat(subject).isConnecting()
        verify { mockStateListener(State.Connecting) }
        verify { mockOnStateChanged(expectedStateChange) }
    }

    @Test
    fun whenOnConnectAfterOnReconnect() {
        val expectedStateChange = StateChange(State.Idle, State.Reconnecting)

        subject.onReconnect()
        subject.onConnect()

        assertThat(subject).isReconnecting()
        verify { mockStateListener(State.Reconnecting) }
        verify { mockOnStateChanged(expectedStateChange) }
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
    fun whenOnReconnect() {
        val expectedStateChange = StateChange(State.Idle, State.Reconnecting)

        subject.onReconnect()

        assertThat(subject).isReconnecting()
        verify { mockStateListener(State.Reconnecting) }
        verify { mockOnStateChanged(expectedStateChange) }
    }

    @Test
    fun whenOnSessionConfigured() {
        val expectedStateChange =
            StateChange(State.Idle, State.Configured(connected = true, newSession = true))

        subject.onSessionConfigured(connected = true, newSession = true)

        assertThat(subject).isConfigured(
            connected = true,
            newSession = true,
        )
        verify {
            mockStateListener(State.Configured(connected = true, newSession = true))
        }
        verify { mockOnStateChanged(expectedStateChange) }
    }

    @Test
    fun whenOnSessionConfiguredAfterOnReconnect() {
        val expectedStateChange =
            StateChange(State.Reconnecting, State.Configured(connected = true, newSession = true))

        subject.onReconnect()
        subject.onSessionConfigured(connected = true, newSession = true)

        assertThat(subject).isConfigured(
            connected = true,
            newSession = true,
        )
        verify {
            mockStateListener(
                State.Configured(
                    connected = true,
                    newSession = true,
                )
            )
        }
        verify { mockOnStateChanged(expectedStateChange) }
    }

    @Test
    fun whenOnClosingAfterConnectionWasOpened() {
        val expectedStateChange =
            StateChange(State.Connected, State.Closing(code = 1, reason = "A reason."))

        subject.onConnectionOpened()
        subject.onClosing(code = 1, reason = "A reason.")

        assertThat(subject).isClosing(code = 1, reason = "A reason.")
        verify { mockStateListener(State.Closing(code = 1, reason = "A reason.")) }
        verify { mockOnStateChanged(expectedStateChange) }
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

        assertThat(subject).isClosed(code = 1, reason = "A reason.")
        verify { mockStateListener(State.Closed(code = 1, reason = "A reason.")) }
        verify { mockOnStateChanged(expectedStateChange) }
    }

    @Test
    fun whenOnError() {
        val expectedStateChange =
            StateChange(State.Idle, State.Error(ErrorCode.WebsocketError, "A message."))

        subject.onError(code = ErrorCode.WebsocketError, message = "A message.")

        assertThat(subject).isError(
            code = ErrorCode.WebsocketError,
            message = "A message."
        )
        verify {
            mockStateListener(
                State.Error(
                    code = ErrorCode.WebsocketError,
                    message = "A message."
                )
            )
        }
        verify { mockOnStateChanged(expectedStateChange) }
    }

    private fun Assert<StateMachine>.currentState() = prop(StateMachine::currentState)
    private fun Assert<StateMachine>.isIdle() = currentState().isEqualTo(State.Idle)
    private fun Assert<StateMachine>.isClosed(code: Int, reason: String) =
        currentState().isEqualTo(State.Closed(code, reason))

    private fun Assert<StateMachine>.isConnecting() =
        currentState().isEqualTo(State.Connecting)

    private fun Assert<StateMachine>.isConnected() =
        currentState().isEqualTo(State.Connected)

    private fun Assert<StateMachine>.isReconnecting() =
        currentState().isEqualTo(State.Reconnecting)

    private fun Assert<StateMachine>.isClosing(code: Int, reason: String) =
        currentState().isEqualTo(State.Closing(code, reason))

    private fun Assert<StateMachine>.isConfigured(
        connected: Boolean,
        newSession: Boolean,
    ) =
        currentState().isEqualTo(
            State.Configured(
                connected,
                newSession,
            )
        )

    private fun Assert<StateMachine>.isError(
        code: ErrorCode,
        message: String?,
    ) =
        currentState().isEqualTo(
            State.Error(
                code, message
            )
        )
}
