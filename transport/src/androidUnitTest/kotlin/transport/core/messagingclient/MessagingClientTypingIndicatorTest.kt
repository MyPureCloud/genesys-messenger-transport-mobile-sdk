package transport.core.messagingclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.TYPING_INDICATOR_COOL_DOWN_MILLISECONDS
import com.genesys.cloud.messenger.transport.core.toCorrectiveAction
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import io.mockk.every
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test
import transport.util.Request
import transport.util.Response
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MessagingClientTypingIndicatorTest : BaseMessagingClientTest() {
    @Test
    fun `when indicateTyping and showUserTyping is enabled`() {
        subject.connect()

        subject.indicateTyping()

        verifySequence {
            connectSequence()
            mockLogger.i(capture(logSlot))
            mockLogger.i(capture(logSlot))
            mockPlatformSocket.sendMessage(match { Request.isUserTypingRequest(it) })
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession(Request.token))
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.INDICATE_TYPING)
        assertThat(logSlot[3].invoke()).isEqualTo(LogMessages.WILL_SEND_MESSAGE)
    }

    @Test
    fun `when indicateTyping and showUserTyping is disabled`() {
        every { mockShowUserTypingIndicatorFunction.invoke() } returns false
        subject.connect()

        subject.indicateTyping()

        verify(exactly = 0) {
            mockPlatformSocket.sendMessage(match { Request.isUserTypingRequest(it) })
        }
        assertNull(userTypingProvider.encodeRequest(token = Request.token))
    }

    @Test
    fun `when not connected and indicateTyping invoked`() {
        assertFailsWith<IllegalStateException> {
            subject.indicateTyping()
        }
    }

    @Test
    fun `when indicateTyping twice without cool down`() {
        subject.connect()

        subject.indicateTyping()
        subject.indicateTyping()

        verify(exactly = 1) { mockPlatformSocket.sendMessage(match { Request.isUserTypingRequest(it) }) }
    }

    @Test
    fun `when indicateTyping twice with cool down`() {
        val typingIndicatorCoolDownInMilliseconds = TYPING_INDICATOR_COOL_DOWN_MILLISECONDS + 250

        subject.connect()

        subject.indicateTyping()
        // Fast forward epochMillis by typingIndicatorCoolDownInMilliseconds.
        every { mockTimestampFunction.invoke() } answers { Platform().epochMillis() + typingIndicatorCoolDownInMilliseconds }
        subject.indicateTyping()

        verify(exactly = 2) { mockPlatformSocket.sendMessage(match { Request.isUserTypingRequest(it) }) }
    }

    @Test
    fun `when indicateTyping twice without cool down but after message was sent`() {
        subject.connect()

        subject.indicateTyping()
        slot.captured.onMessage(Response.onMessage())
        subject.indicateTyping()

        verify(exactly = 2) { mockPlatformSocket.sendMessage(match { Request.isUserTypingRequest(it) }) }
    }

    @Test
    fun `when WebSocket respond with typing indicator forbidden error`() {
        val expectedErrorCode = ErrorCode.ClientResponseError(403)
        val expectedEvent =
            Event.Error(
                errorCode = expectedErrorCode,
                message = "Turn on the Feature Toggle or fix the configuration.",
                correctiveAction = expectedErrorCode.toCorrectiveAction(),
            )
        subject.connect()

        slot.captured.onMessage(Response.typingIndicatorForbidden)

        verify { mockEventHandler.onEvent(expectedEvent) }
    }
}
