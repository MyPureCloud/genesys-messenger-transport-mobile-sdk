package com.genesys.cloud.messenger.transport.core.events

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
        val givenStructuredMessageEvent = TypingEvent(Typing(type = "On", duration = 5000))
        val expectedEvent = Event.Typing(5000)

        subject.onEvent(givenStructuredMessageEvent)

        verify { mockEventListener.invoke(eq(expectedEvent)) }
    }

    @Test
    fun whenNoEventListenerSetAndOnEventInvoked() {
        val givenStructuredMessageEvent = TypingEvent(Typing(type = "On", duration = 5000))
        subject.eventListener = null

        subject.onEvent(givenStructuredMessageEvent)

        verify(exactly = 0) { mockEventListener.invoke(any()) }
    }
}
