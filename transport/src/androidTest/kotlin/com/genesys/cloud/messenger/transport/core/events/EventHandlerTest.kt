package com.genesys.cloud.messenger.transport.core.events

import com.genesys.cloud.messenger.transport.core.CorrectiveAction
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.shyrka.receive.ErrorEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.TypingEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.TypingEvent.Typing
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class EventHandlerTest {
    private val mockEventListener: ((Event) -> Unit) = mockk(relaxed = true)
    private val subject = EventHandlerImpl().also {
        it.eventListener = mockEventListener
    }

    @Test
    fun whenTypingEventOccurs() {
        val givenStructuredMessageEvent = TypingEvent(
            eventType = StructuredMessageEvent.Type.Typing,
            typing = Typing(type = "On", duration = 3000)
        )
        val expectedEvent = Event.AgentTyping(3000)

        subject.onEvent(givenStructuredMessageEvent)

        verify { mockEventListener.invoke(eq(expectedEvent)) }
    }

    @Test
    fun whenNoEventListenerSetAndOnEventInvoked() {
        val givenStructuredMessageEvent = TypingEvent(
            eventType = StructuredMessageEvent.Type.Typing,
            typing = Typing(type = "On", duration = 5000)
        )
        subject.eventListener = null

        subject.onEvent(givenStructuredMessageEvent)

        verify(exactly = 0) { mockEventListener.invoke(any()) }
    }

    @Test
    fun whenTypingEventWithNullDuration() {
        val givenStructuredMessageEvent = TypingEvent(
            eventType = StructuredMessageEvent.Type.Typing,
            typing = Typing(type = "On", duration = null)
        )
        val expectedEvent = Event.AgentTyping(5000)

        subject.onEvent(givenStructuredMessageEvent)

        verify { mockEventListener.invoke(eq(expectedEvent)) }
    }

    @Test
    fun whenErrorEvent() {
        val givenStructuredMessageEvent = ErrorEvent(
            errorCode = ErrorCode.ClientResponseError(403),
            message = "some message",
        )

        val expectedEvent = Event.Error(
            errorCode = ErrorCode.ClientResponseError(403),
            message = "some message",
            correctiveAction = CorrectiveAction.Forbidden,
        )

        subject.onEvent(givenStructuredMessageEvent)

        verify { mockEventListener.invoke(eq(expectedEvent)) }
    }
}
