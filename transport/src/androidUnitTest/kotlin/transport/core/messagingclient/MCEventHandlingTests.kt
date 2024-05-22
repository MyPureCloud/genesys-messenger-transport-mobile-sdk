package com.genesys.cloud.messenger.transport.core.messagingclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.core.MessagingClient
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.util.Response
import com.genesys.cloud.messenger.transport.util.fromConfiguredToError
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test

class MCEventHandlingTests : BaseMessagingClientTest() {

    @Test
    fun `when eventListener is set`() {
        val givenEventListener: (Event) -> Unit = {}

        subject.eventListener = givenEventListener

        verify {
            mockEventHandler.eventListener = givenEventListener
        }
    }

    @Test
    fun `when eventListener is not set`() {
        assertThat(subject.eventListener).isNull()
    }

    @Test
    fun `when StructuredMessage with outbound multiple events is received`() {
        val firstExpectedEvent = Event.AgentTyping(1000)
        val secondsExpectedEvent = Event.AgentTyping(5000)

        subject.connect()

        slot.captured.onMessage(Response.structuredMessageWithEvents())

        verifySequence {
            mockEventHandler.onEvent(eq(firstExpectedEvent))
            mockEventHandler.onEvent(eq(secondsExpectedEvent))
        }
        verify(exactly = 0) { mockMessageStore.update(any()) }
        verify(exactly = 0) { mockAttachmentHandler.onSent(any()) }
    }

    @Test
    fun `when StructuredMessage with outbound Unknown event type is received`() {
        subject.connect()

        slot.captured.onMessage(Response.structuredMessageWithEvents(Response.StructuredEvent.unknown))

        verify(exactly = 0) { mockEventHandler.onEvent(any()) }
        verify(exactly = 0) { mockMessageStore.update(any()) }
        verify(exactly = 0) { mockAttachmentHandler.onSent(any()) }
    }

    @Test
    fun `when StructuredMessage with inbound event is received`() {
        subject.connect()

        slot.captured.onMessage(Response.structuredMessageWithEvents(direction = Message.Direction.Inbound))

        verify { mockLogger.i(capture(logSlot)) }
        verify(exactly = 0) { mockEventHandler.onEvent(any()) }
        verify(exactly = 0) { mockMessageStore.update(any()) }
        verify(exactly = 0) { mockAttachmentHandler.onSent(any()) }
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.ignoreInboundEvent(Event.AgentTyping(1000)))
        assertThat(logSlot[3].invoke()).isEqualTo(LogMessages.ignoreInboundEvent(Event.AgentTyping(5000)))
    }

    @Test
    fun `when event ConnectionClosed is received`() {
        val expectedEvent = Event.ConnectionClosed

        subject.connect()
        slot.captured.onMessage(Response.connectionClosedEvent)

        verifySequence {
            connectSequence()
            disconnectSequence()
            mockEventHandler.onEvent(eq(expectedEvent))
        }
    }

    @Test
    fun `when event SessionExpired is received`() {
        val expectedErrorState = MessagingClient.State.Error(ErrorCode.SessionHasExpired, null)

        subject.connect()

        slot.captured.onMessage(Response.sessionExpiredEvent)

        verifySequence {
            connectSequence()
            errorSequence(fromConfiguredToError(expectedErrorState))
        }
    }

    @Test
    fun `when StructuredMessage with inbound event PresenceJoin is received`() {
        val givenStructuredMessage = Response.structuredMessageWithEvents(
            direction = Message.Direction.Inbound,
            events = Response.StructuredEvent.presenceJoin,
        )
        subject.connect()

        slot.captured.onMessage(givenStructuredMessage)

        verifySequence {
            connectSequence()
            mockCustomAttributesStore.onSent()
            mockEventHandler.onEvent(Event.ConversationAutostart)
        }
    }

    @Test
    fun `when StructuredMessage with inbound event SignIn is received`() {
        val givenStructuredMessage = Response.structuredMessageWithEvents(
            direction = Message.Direction.Inbound,
            events = Response.StructuredEvent.presenceSignIn,
        )
        subject.connect()

        slot.captured.onMessage(givenStructuredMessage)

        verifySequence {
            connectSequence()
            mockEventHandler.onEvent(Event.SignedIn())
        }
    }
}
