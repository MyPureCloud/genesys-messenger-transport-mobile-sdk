package com.genesys.cloud.messenger.transport.core.messagingclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.DEFAULT_PAGE_SIZE
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.shyrka.receive.JwtResponse
import com.genesys.cloud.messenger.transport.util.Response
import com.genesys.cloud.messenger.transport.utility.LogMessages
import io.mockk.every
import io.mockk.verify
import io.mockk.verifySequence
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
            mockLogger.i(capture(logSlot))
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.Connect)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.ConfigureSession)
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.HistoryFetched)
    }

    @Test
    fun `when SocketListener invoke onMessage with JwtResponse message`() {
        subject.connect()

        slot.captured.onMessage(Response.jwtResponse)

        verifySequence {
            connectSequence()
            mockJwtHandler.jwtResponse = JwtResponse("some_jwt", 333)
        }
    }
}
