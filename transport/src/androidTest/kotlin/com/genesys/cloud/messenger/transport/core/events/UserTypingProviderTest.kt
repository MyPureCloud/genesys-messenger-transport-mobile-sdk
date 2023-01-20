package com.genesys.cloud.messenger.transport.core.events

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.Request
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Test

class UserTypingProviderTest {
    private val mockTimestampFunction: () -> Long = spyk<() -> Long>().also {
        every { it.invoke() } answers { Platform().epochMillis() }
    }
    private val mockShowUserTypingIndicatorFunction: () -> Boolean = spyk<() -> Boolean>().also {
        every { it.invoke() } returns true
    }

    private val subject = UserTypingProvider(
        log = mockk(relaxed = true),
        showUserTypingEnabled = mockShowUserTypingIndicatorFunction,
        getCurrentTimestamp = mockTimestampFunction,
    )

    @Test
    fun whenEncode() {
        val expected = Request.userTypingRequest
        val result = subject.encodeRequest(token = Request.token)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun whenEncodeWithCoolDown() {
        val typingIndicatorCoolDownInMilliseconds = UserTypingProvider.TYPING_INDICATOR_COOL_DOWN_IN_MILLISECOND + 250
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

        assertThat(firstResult).isEqualTo(expected)
        assertThat(secondResult).isNull()
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

        assertThat(result).isNull()
    }
}
