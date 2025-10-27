package transport.core.messagingclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.shyrka.receive.JwtResponse
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import io.mockk.coVerify
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.runBlocking
import org.junit.Test
import transport.util.Request
import transport.util.Response
import kotlin.test.assertFailsWith

class MessagingClientHistoryTest : BaseMessagingClientTest() {

    @Test
    fun `when fetchNextPage but session is not configured`() {
        assertFailsWith<IllegalStateException> { runBlocking { subject.fetchNextPage() } }
    }

    @Test
    fun `when fetchNextPage and session is configured`() {
        subject.connect()

        runBlocking { subject.fetchNextPage() }

        coVerify { mockHistoryHandler.fetchNextPage() }
        verify { mockLogger.i(capture(logSlot)) }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession(Request.token))
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
