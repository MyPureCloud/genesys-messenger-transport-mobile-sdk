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

class HealthCheckProviderTest {
    private val mockTimestampFunction: () -> Long = spyk<() -> Long>().also {
        every { it.invoke() } answers { Platform().epochMillis() }
    }

    private val subject = HealthCheckProvider(mockk(relaxed = true), mockTimestampFunction)

    @Test
    fun whenEncode() {
        val expected = Request.echoRequest

        val result = subject.encodeRequest("00000000-0000-0000-0000-000000000000")

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun whenEncodeWithCoolDown() {
        val healthCheckCoolDownInMilliseconds = 30000
        val expected = Request.echoRequest

        val firstResult = subject.encodeRequest("00000000-0000-0000-0000-000000000000")
        every { mockTimestampFunction.invoke() } answers { Platform().epochMillis() + healthCheckCoolDownInMilliseconds }
        val secondResult = subject.encodeRequest("00000000-0000-0000-0000-000000000000")

        assertThat(firstResult).isEqualTo(expected)
        assertThat(secondResult).isEqualTo(expected)
    }

    @Test
    fun whenEncodeWithoutCoolDown() {
        val expected = Request.echoRequest

        val firstResult = subject.encodeRequest("00000000-0000-0000-0000-000000000000")
        val secondResult = subject.encodeRequest("00000000-0000-0000-0000-000000000000")

        assertThat(firstResult).isEqualTo(expected)
        assertThat(secondResult).isNull()
    }

    @Test
    fun whenEncodeWithoutCoolDownButWithClear() {
        val expected = Request.echoRequest

        val firstResult = subject.encodeRequest("00000000-0000-0000-0000-000000000000")
        subject.clear()
        val secondResult = subject.encodeRequest("00000000-0000-0000-0000-000000000000")

        assertThat(firstResult).isEqualTo(expected)
        assertThat(secondResult).isEqualTo(expected)
    }
}
