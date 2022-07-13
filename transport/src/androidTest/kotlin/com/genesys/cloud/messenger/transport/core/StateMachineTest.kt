package com.genesys.cloud.messenger.transport.core

import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import kotlin.test.Test

class StateMachineTest {
    private val subject = StateMachineImpl()
    private val mockStateListener: (MessagingClient.State) -> Unit = spyk()

    @Before
    fun setup() {
        subject.stateListener = mockStateListener
    }

    @Test
    fun whenSubjectWasInitialized() {
        assertThat(subject).isIdle()
    }

    @Test
    fun whenOnConnectionOpened() {
        subject.onConnectionOpened()

        assertThat(subject).isConnected()

        verify { mockStateListener(MessagingClient.State.Connected) }
    }

    @Test
    fun whenOnConnectionOpenedAfterOnReconnect() {
        subject.onReconnect()
        subject.onConnectionOpened()

        assertThat(subject).isReconnecting()
        verify { mockStateListener(MessagingClient.State.Reconnecting) }
    }

    @Test
    fun whenOnConnect() {
        subject.onConnect()

        assertThat(subject).isConnecting()
        verify { mockStateListener(MessagingClient.State.Connecting) }
    }

    @Test
    fun whenOnConnectAfterOnReconnect() {
        subject.onReconnect()
        subject.onConnect()

        assertThat(subject).isReconnecting()
        verify { mockStateListener(MessagingClient.State.Reconnecting) }
    }

    @Test
    fun whenOnReconnect() {
        subject.onReconnect()

        assertThat(subject).isReconnecting()
        verify { mockStateListener(MessagingClient.State.Reconnecting) }
    }

    @Test
    fun whenOnSessionConfigured() {
        subject.onSessionConfigured(connected = true, newSession = true)

        assertThat(subject).isConfigured(
            connected = true,
            newSession = true,
            wasReconnecting = false
        )
        verify {
            mockStateListener(
                MessagingClient.State.Configured(
                    connected = true,
                    newSession = true,
                    wasReconnecting = false
                )
            )
        }
    }

    @Test
    fun whenOnSessionConfiguredAfterOnReconnect() {
        subject.onReconnect()
        subject.onSessionConfigured(connected = true, newSession = true)

        assertThat(subject).isConfigured(
            connected = true,
            newSession = true,
            wasReconnecting = true
        )
        verify {
            mockStateListener(
                MessagingClient.State.Configured(
                    connected = true,
                    newSession = true,
                    wasReconnecting = true
                )
            )
        }
    }

    @Test
    fun whenOnClosing() {
        subject.onClosing(code = 1, reason = "A reason.")

        assertThat(subject).isClosing(code = 1, reason = "A reason.")
        verify { mockStateListener(MessagingClient.State.Closing(code = 1, reason = "A reason.")) }
    }

    @Test
    fun whenOnClosed() {
        subject.onClosed(code = 1, reason = "A reason.")

        assertThat(subject).isClosed(code = 1, reason = "A reason.")
        verify { mockStateListener(MessagingClient.State.Closed(code = 1, reason = "A reason.")) }
    }

    @Test
    fun whenOnError() {
        subject.onError(code = ErrorCode.WebsocketError, message = "A message.")

        assertThat(subject).isError(
            code = ErrorCode.WebsocketError,
            message = "A message."
        )
        verify {
            mockStateListener(
                MessagingClient.State.Error(
                    code = ErrorCode.WebsocketError,
                    message = "A message."
                )
            )
        }
    }

    private fun Assert<StateMachine>.currentState() = prop(StateMachine::currentState)
    private fun Assert<StateMachine>.isIdle() = currentState().isEqualTo(MessagingClient.State.Idle)
    private fun Assert<StateMachine>.isClosed(code: Int, reason: String) =
        currentState().isEqualTo(MessagingClient.State.Closed(code, reason))

    private fun Assert<StateMachine>.isConnecting() =
        currentState().isEqualTo(MessagingClient.State.Connecting)

    private fun Assert<StateMachine>.isConnected() =
        currentState().isEqualTo(MessagingClient.State.Connected)

    private fun Assert<StateMachine>.isReconnecting() =
        currentState().isEqualTo(MessagingClient.State.Reconnecting)

    private fun Assert<StateMachine>.isClosing(code: Int, reason: String) =
        currentState().isEqualTo(MessagingClient.State.Closing(code, reason))

    private fun Assert<StateMachine>.isConfigured(
        connected: Boolean,
        newSession: Boolean,
        wasReconnecting: Boolean,
    ) =
        currentState().isEqualTo(
            MessagingClient.State.Configured(
                connected,
                newSession,
                wasReconnecting
            )
        )

    private fun Assert<StateMachine>.isError(
        code: ErrorCode,
        message: String?,
    ) =
        currentState().isEqualTo(
            MessagingClient.State.Error(
                code, message
            )
        )
}
