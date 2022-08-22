package com.genesys.cloud.messenger.transport.core.events

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.Request
import io.mockk.every
import io.mockk.spyk
import org.junit.Test

class UserTypingProviderTest {
    private val mockTimestampFunction: () -> Long = spyk<() -> Long>().also {
        every { it.invoke() } answers { Platform().epochMillis() }
    }

    private val subject = UserTypingProvider(mockTimestampFunction)

    @Test
    fun whenEncode() {
        val expected = Request.userTypingRequest

        val result = subject.encodeRequest("00000000-0000-0000-0000-000000000000")

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun whenEncodeWithCoolDown() {
        val expected = Request.userTypingRequest

        val firstResult = subject.encodeRequest("00000000-0000-0000-0000-000000000000")
        every { mockTimestampFunction.invoke() } answers { Platform().epochMillis() + TYPING_INDICATOR_COOL_DOWN_IN_MILLISECOND }
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
}
