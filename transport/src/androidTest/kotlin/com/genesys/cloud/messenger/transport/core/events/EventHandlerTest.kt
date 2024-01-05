package com.genesys.cloud.messenger.transport.core.events

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.CorrectiveAction
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.events.Event.AgentTyping
import com.genesys.cloud.messenger.transport.core.events.Event.Authorized
import com.genesys.cloud.messenger.transport.core.events.Event.ConnectionClosed
import com.genesys.cloud.messenger.transport.core.events.Event.ConversationAutostart
import com.genesys.cloud.messenger.transport.core.events.Event.ConversationCleared
import com.genesys.cloud.messenger.transport.core.events.Event.ConversationDisconnect
import com.genesys.cloud.messenger.transport.core.events.Event.Error
import com.genesys.cloud.messenger.transport.core.events.Event.HealthChecked
import com.genesys.cloud.messenger.transport.core.events.Event.Logout
import com.genesys.cloud.messenger.transport.shyrka.receive.PresenceEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.TypingEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.TypingEvent.Typing
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.utility.ErrorTest
import com.genesys.cloud.messenger.transport.utility.LogMessages
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertNull

class EventHandlerTest {
    internal val mockLogger: Log = mockk(relaxed = true)
    internal val logSlot = mutableListOf<() -> String>()
    private val mockEventListener: ((Event) -> Unit) = mockk(relaxed = true)
    private val subject = EventHandlerImpl(mockLogger).also {
        it.eventListener = mockEventListener
    }

    @Test
    fun whenOnEvent() {
        val events = listOf(
            AgentTyping(3000),
            HealthChecked,
            Error(
                errorCode = ErrorCode.ClientResponseError(403),
                message = "some message",
                correctiveAction = CorrectiveAction.Forbidden,
            ),
            ConversationAutostart,
            ConversationDisconnect,
            ConnectionClosed,
            Authorized,
            Logout,
            ConversationCleared,
        )

        events.forEach {
            subject.onEvent(it)

            verify {
                mockLogger.i(capture(logSlot))
                mockEventListener.invoke(eq(it))
            }
        }
    }

    @Test
    fun whenNoEventListenerSetAndOnEventInvoked() {
        subject.eventListener = null

        subject.onEvent(HealthChecked)

        verify(exactly = 0) { mockEventListener.invoke(any()) }
    }

    @Test
    fun whenOnEventNullInvoked() {
        subject.onEvent(null)

        verify { mockLogger.i(capture(logSlot)) }
        verify(exactly = 0) { mockEventListener.invoke(any()) }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.UnknownEvent)
    }

    @Test
    fun whenTypingEventToTransportEvent() {
        val expectedEvent = AgentTyping(3000)

        val result = TypingEvent(
            eventType = StructuredMessageEvent.Type.Typing,
            typing = Typing(type = "On", duration = 3000)
        ).toTransportEvent()

        assertThat(result).isEqualTo(expectedEvent)
    }

    @Test
    fun whenTypingEventWithNullDurationToTransportEvent() {
        val expectedEvent = AgentTyping(5000)

        val result = TypingEvent(
            eventType = StructuredMessageEvent.Type.Typing,
            typing = Typing(type = "On", duration = null)
        ).toTransportEvent()

        assertThat(result).isEqualTo(expectedEvent)
    }

    @Test
    fun whenPresenceEventJoinToTransportEvent() {
        val expectedEvent = ConversationAutostart

        val result = PresenceEvent(
            StructuredMessageEvent.Type.Presence,
            PresenceEvent.Presence(
                PresenceEvent.Presence.Type.Join
            )
        ).toTransportEvent()

        assertThat(result).isEqualTo(expectedEvent)
    }

    @Test
    fun whenPresenceEventDisconnectToTransportEvent() {
        val expectedEvent = ConversationDisconnect

        val result = PresenceEvent(
            StructuredMessageEvent.Type.Presence,
            PresenceEvent.Presence(
                PresenceEvent.Presence.Type.Disconnect
            )
        ).toTransportEvent()

        assertThat(result).isEqualTo(expectedEvent)
    }

    @Test
    fun whenPresenceEventClearToTransportEvent() {
        val result = PresenceEvent(
            StructuredMessageEvent.Type.Presence,
            PresenceEvent.Presence(
                PresenceEvent.Presence.Type.Clear
            )
        ).toTransportEvent()

        assertNull(result)
    }

    @Test
    fun `validate event Error payload`() {
        val expectedErrorCodePayload = ErrorCode.UnexpectedError
        val expectedErrorMessagePayload = ErrorTest.Message
        val expectedCorrectiveActionPayload = CorrectiveAction.Unknown
        val expectedErrorEvent = Error(expectedErrorCodePayload,expectedErrorMessagePayload, expectedCorrectiveActionPayload)
        val givenErrorEvent =
            Error(ErrorCode.UnexpectedError, ErrorTest.Message, CorrectiveAction.Unknown)

        subject.onEvent(givenErrorEvent)

        verify {
            mockLogger.i(capture(logSlot))
            mockEventListener.invoke(eq(expectedErrorEvent))
        }

        givenErrorEvent.run {
            assertThat(errorCode).isEqualTo(expectedErrorCodePayload)
            assertThat(message).isEqualTo(expectedErrorMessagePayload)
            assertThat(correctiveAction).isEqualTo(expectedCorrectiveActionPayload)
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.onEvent(expectedErrorEvent))

    }

    @Test
    fun `validate event AgentTyping payload`() {
        val expectedAgentTypingPayload = 100L
        val expectedAgentTypingEvent = AgentTyping(expectedAgentTypingPayload)
        val givenAgentTypingEvent = AgentTyping(100)

        subject.onEvent(givenAgentTypingEvent)

        verify {
            mockLogger.i(capture(logSlot))
            mockEventListener.invoke(eq(expectedAgentTypingEvent))
        }
        assertThat(givenAgentTypingEvent.durationInMilliseconds).isEqualTo(expectedAgentTypingPayload)
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.onEvent(expectedAgentTypingEvent))
    }
}
