package com.genesys.cloud.messenger.transport.util

import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.core.MessageDispatcher
import com.genesys.cloud.messenger.transport.core.MessageEvent
import com.genesys.cloud.messenger.transport.core.MessageListener
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
