package transport.core.events

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.genesys.cloud.messenger.transport.core.events.HEALTH_CHECK_COOL_DOWN_MILLISECONDS
import com.genesys.cloud.messenger.transport.core.events.HealthCheckProvider
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.util.logs.LogTag
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test
import transport.util.Request
import kotlin.test.assertTrue

class HealthCheckProviderTest {
    internal val mockLogger: Log = mockk(relaxed = true)
    internal val logSlot = mutableListOf<() -> String>()
    private val mockTimestampFunction: () -> Long = spyk<() -> Long>().also {
        every { it.invoke() } answers { Platform().epochMillis() }
    }

    private val subject = HealthCheckProvider(mockLogger, mockTimestampFunction)

    @Test
    fun whenEncode() {
        val expected = Request.echo
        val result = subject.encodeRequest(token = Request.token)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun whenEncodeWithCoolDown() {
        val healthCheckCoolDownInMilliseconds = HEALTH_CHECK_COOL_DOWN_MILLISECONDS + 250
        val expected = Request.echo
        val firstResult = subject.encodeRequest(token = Request.token)
        every { mockTimestampFunction.invoke() } answers { Platform().epochMillis() + healthCheckCoolDownInMilliseconds }
        val secondResult = subject.encodeRequest(token = Request.token)

        verifySequence {
            mockTimestampFunction.invoke()
            mockTimestampFunction.invoke()
        }
        assertThat(firstResult).isEqualTo(expected)
        assertThat(secondResult).isEqualTo(expected)
    }

    @Test
    fun whenEncodeWithoutCoolDown() {
        val expected = Request.echo
        val firstResult = subject.encodeRequest(token = Request.token)
        val secondResult = subject.encodeRequest(token = Request.token)

        verify {
            mockLogger.w(capture(logSlot))
        }
        assertThat(firstResult).isEqualTo(expected)
        assertThat(secondResult).isNull()
        assertThat(logSlot[0].invoke()).isEqualTo(
            LogMessages.healthCheckCoolDown(
                HEALTH_CHECK_COOL_DOWN_MILLISECONDS
            )
        )
    }

    @Test
    fun whenEncodeWithoutCoolDownButWithClear() {
        val expected = Request.echo
        val firstResult = subject.encodeRequest(token = Request.token)
        subject.clear()
        val secondResult = subject.encodeRequest(token = Request.token)

        assertThat(firstResult).isEqualTo(expected)
        assertThat(secondResult).isEqualTo(expected)
    }

    @Test
    fun `when getCurrentTimestamp()`() {
        val givenAcceptableRangeOffset = 10

        val result = System.currentTimeMillis()

        assertTrue { subject.getCurrentTimestamp() in (result - givenAcceptableRangeOffset)..(result + givenAcceptableRangeOffset) }
    }

    @Test
    fun `validate default constructor`() {
        val givenAcceptableRangeOffset = 10
        val currentTime = System.currentTimeMillis()

        val subject = HealthCheckProvider()

        assertThat(subject.log.logger.tag).isEqualTo(LogTag.HEALTH_CHECK_PROVIDER)
        assertTrue { subject.getCurrentTimestamp() in (currentTime - givenAcceptableRangeOffset)..(currentTime + givenAcceptableRangeOffset) }
    }
}
