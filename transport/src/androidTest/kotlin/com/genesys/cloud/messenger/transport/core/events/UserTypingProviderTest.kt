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

        val result = subject.encodeRequest("00000000-0000-0000-0000-000000000000")

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun whenEncodeWithCoolDown() {
        val typingIndicatorCoolDownInMilliseconds = 5000
        val expected = Request.userTypingRequest

        val firstResult = subject.encodeRequest("00000000-0000-0000-0000-000000000000")
        every { mockTimestampFunction.invoke() } answers { Platform().epochMillis() + typingIndicatorCoolDownInMilliseconds }
        val secondResult = subject.encodeRequest("00000000-0000-0000-0000-000000000000")

        assertThat(firstResult).isEqualTo(expected)
        assertThat(secondResult).isEqualTo(expected)
    }

    @Test
    fun whenEncodeWithoutCoolDown() {
        val expected = Request.userTypingRequest

        val firstResult = subject.encodeRequest("00000000-0000-0000-0000-000000000000")
        val secondResult = subject.encodeRequest("00000000-0000-0000-0000-000000000000")

        assertThat(firstResult).isEqualTo(expected)
        assertThat(secondResult).isNull()
    }

    @Test
    fun whenEncodeWithoutCoolDownButWithClear() {
        val expected = Request.userTypingRequest

        val firstResult = subject.encodeRequest("00000000-0000-0000-0000-000000000000")
        subject.clear()
        val secondResult = subject.encodeRequest("00000000-0000-0000-0000-000000000000")

        assertThat(firstResult).isEqualTo(expected)
        assertThat(secondResult).isEqualTo(expected)
    }

    @Test
    fun whenEncodeAndShowUserTypingIsDisabled() {
        every { mockShowUserTypingIndicatorFunction.invoke() } returns false

        val result = subject.encodeRequest("00000000-0000-0000-0000-000000000000")

        assertThat(result).isNull()
    }
}
