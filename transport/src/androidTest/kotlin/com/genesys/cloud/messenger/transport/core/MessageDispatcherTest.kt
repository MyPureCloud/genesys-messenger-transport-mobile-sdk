package com.genesys.cloud.messenger.transport.core

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class MessageDispatcherTest {
    private val mockListener = mockk<MessageListener>(relaxed = true)
    private val subject = MessageDispatcher(mockListener)

    @Test
    fun whenDispatchCalled() {
        val givenEvent = MessageEvent.MessageInserted(Message())

        subject.dispatch(givenEvent)

        verify {
            mockListener.onEvent(givenEvent)
        }
    }
}
