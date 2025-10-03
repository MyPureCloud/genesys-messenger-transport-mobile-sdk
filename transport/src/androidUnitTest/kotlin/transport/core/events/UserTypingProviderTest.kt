package transport.core.events

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.genesys.cloud.messenger.transport.core.events.TYPING_INDICATOR_COOL_DOWN_MILLISECONDS
import com.genesys.cloud.messenger.transport.core.events.UserTypingProvider
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import transport.util.Request

class UserTypingProviderTest {
    private val mockLogger: Log = mockk(relaxed = true)
    private val logSlot = mutableListOf<() -> String>()
    private val mockTimestampFunction: () -> Long = spyk<() -> Long>().also {
        every { it.invoke() } answers { Platform().epochMillis() }
    }
    private val mockShowUserTypingIndicatorFunction: () -> Boolean = spyk<() -> Boolean>().also {
        every { it.invoke() } returns true
    }

    private val subject = UserTypingProvider(
        log = mockLogger,
        showUserTypingEnabled = mockShowUserTypingIndicatorFunction,
        getCurrentTimestamp = mockTimestampFunction,
        tracingIdProvider = mockk { every { getTracingId() } returns "test-tracing-id" },
    )

    @Test
    fun `when encode`() {
        val expected = Request.userTypingRequest
        val result = subject.encodeRequest(token = Request.token)

        verify {
            mockShowUserTypingIndicatorFunction.invoke()
            mockTimestampFunction.invoke()
        }
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `when encode with coolDown`() {
        val typingIndicatorCoolDownInMilliseconds = TYPING_INDICATOR_COOL_DOWN_MILLISECONDS + 250
        val expected = Request.userTypingRequest
        val firstResult = subject.encodeRequest(token = Request.token)
        every { mockTimestampFunction.invoke() } answers { Platform().epochMillis() + typingIndicatorCoolDownInMilliseconds }
        val secondResult = subject.encodeRequest(token = Request.token)

        assertThat(firstResult).isEqualTo(expected)
        assertThat(secondResult).isEqualTo(expected)
    }

    @Test
    fun `when encode without coolDown`() {
        val expected = Request.userTypingRequest
        val firstResult = subject.encodeRequest(token = Request.token)
        val secondResult = subject.encodeRequest(token = Request.token)

        verify {
            mockShowUserTypingIndicatorFunction.invoke()
            mockShowUserTypingIndicatorFunction.invoke()
            mockLogger.w(capture(logSlot))
        }

        assertThat(firstResult).isEqualTo(expected)
        assertThat(secondResult).isNull()
        assertThat(logSlot[0].invoke()).isEqualTo(
            LogMessages.typingIndicatorCoolDown(
                TYPING_INDICATOR_COOL_DOWN_MILLISECONDS
            )
        )
    }

    @Test
    fun `when encode without coolDown but with clear`() {
        val expected = Request.userTypingRequest
        val firstResult = subject.encodeRequest(token = Request.token)
        subject.clear()
        val secondResult = subject.encodeRequest(token = Request.token)

        assertThat(firstResult).isEqualTo(expected)
        assertThat(secondResult).isEqualTo(expected)
    }

    @Test
    fun `when encode and showUserTyping is disabled`() {
        every { mockShowUserTypingIndicatorFunction.invoke() } returns false

        val result = subject.encodeRequest(token = Request.token)

        verify {
            mockShowUserTypingIndicatorFunction.invoke()
            mockLogger.w(capture(logSlot))
        }

        assertThat(result).isNull()
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.TYPING_INDICATOR_DISABLED)
    }

    @Test
    fun `when encode with default getCurrentTimestamp function`() {
        val subject = UserTypingProvider(
            log = mockLogger,
            showUserTypingEnabled = { true },
            tracingIdProvider = mockk { every { getTracingId() } returns "test-tracing-id" }
        )

        val result = subject.encodeRequest(token = Request.token)

        assertThat(result).isEqualTo(Request.userTypingRequest)
    }
}
