package com.genesys.cloud.messenger.transport.core.messagingclient

import com.genesys.cloud.messenger.transport.core.DEFAULT_PAGE_SIZE
import com.genesys.cloud.messenger.transport.core.Message
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertFailsWith

class MCHistoryTests : BaseMessagingClientTest() {

    @Test
    fun `when fetchNextPage but session is not configured`() {
        assertFailsWith<IllegalStateException> { runBlocking { subject.fetchNextPage() } }
    }

    @Test
    fun `when fetchNextPage but all history was already fetched`() {
        every { mockMessageStore.startOfConversation } returns true
        every { mockMessageStore.getConversation() } returns List(DEFAULT_PAGE_SIZE) { Message() }
        subject.connect()

        runBlocking { subject.fetchNextPage() }

        verify {
            mockMessageStore.updateMessageHistory(emptyList(), DEFAULT_PAGE_SIZE)
        }
    }
}
