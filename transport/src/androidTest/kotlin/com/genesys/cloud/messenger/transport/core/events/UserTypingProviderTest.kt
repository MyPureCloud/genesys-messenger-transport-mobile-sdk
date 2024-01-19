package com.genesys.cloud.messenger.transport.core.events

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.Request
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.utility.LogMessages
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test

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
    )

    @Test
    fun whenEncode() {
        val expected = Request.userTypingRequest
        val result = subject.encodeRequest(token = Request.token)

        verify {
            mockShowUserTypingIndicatorFunction.invoke()
            mockTimestampFunction.invoke()
        }
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun whenEncodeWithCoolDown() {
        val typingIndicatorCoolDownInMilliseconds = TYPING_INDICATOR_COOL_DOWN_MILLISECONDS + 250
        val expected = Request.userTypingRequest
        val firstResult = subject.encodeRequest(token = Request.token)
        every { mockTimestampFunction.invoke() } answers { Platform().epochMillis() + typingIndicatorCoolDownInMilliseconds }
        val secondResult = subject.encodeRequest(token = Request.token)

        assertThat(firstResult).isEqualTo(expected)
        assertThat(secondResult).isEqualTo(expected)
    }

    @Test
    fun whenEncodeWithoutCoolDown() {
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
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.TypingCoolDown)
    }

    @Test
    fun whenEncodeWithoutCoolDownButWithClear() {
        val expected = Request.userTypingRequest
        val firstResult = subject.encodeRequest(token = Request.token)
        subject.clear()
        val secondResult = subject.encodeRequest(token = Request.token)

        assertThat(firstResult).isEqualTo(expected)
        assertThat(secondResult).isEqualTo(expected)
    }

    @Test
    fun whenEncodeAndShowUserTypingIsDisabled() {
        every { mockShowUserTypingIndicatorFunction.invoke() } returns false

        val result = subject.encodeRequest(token = Request.token)

        verify {
            mockShowUserTypingIndicatorFunction.invoke()
            mockLogger.w(capture(logSlot))
        }

        assertThat(result).isNull()
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.TypingDisabled)
    }
}
