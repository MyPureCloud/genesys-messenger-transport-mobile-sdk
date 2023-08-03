package com.genesys.cloud.messenger.transport.core.messagingclient

import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.core.MessagingClient
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.util.Response
import com.genesys.cloud.messenger.transport.util.fromConfiguredToError
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test

class MCEventHandlingTests : BaseMessagingClientTest() {

    @Test
    fun `when StructuredMessage with multiple events is received`() {
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
    fun `when StructuredMessage with Unknown event type is received`() {
        val givenUnknownEvent = """{"eventType": "Fake","bloop": {"bip": "bop"}}"""
        subject.connect()

        slot.captured.onMessage(Response.structuredMessageWithEvents(givenUnknownEvent))

        verify { mockEventHandler.onEvent(null) }
        verify(exactly = 0) { mockMessageStore.update(any()) }
        verify(exactly = 0) { mockAttachmentHandler.onSent(any()) }
    }

    @Test
    fun `when StructuredMessage with inbound event is received`() {
        subject.connect()

        slot.captured.onMessage(Response.structuredMessageWithEvents(direction = Message.Direction.Inbound))

        verify(exactly = 0) { mockEventHandler.onEvent(any()) }
        verify(exactly = 0) { mockMessageStore.update(any()) }
        verify(exactly = 0) { mockAttachmentHandler.onSent(any()) }
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
}
