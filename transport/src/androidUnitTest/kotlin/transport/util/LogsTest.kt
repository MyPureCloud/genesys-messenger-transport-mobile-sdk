package transport.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.genesys.cloud.messenger.transport.util.logs.KtorLogger
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.util.logs.LogTag
import com.genesys.cloud.messenger.transport.util.logs.Logger
import com.genesys.cloud.messenger.transport.util.logs.okHttpLogger
import com.genesys.cloud.messenger.transport.utility.ErrorTest
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class LogsTest {
    private val mockLogger: Logger =
        mockk(relaxed = true) {
            every { tag } returns TestValues.LOG_TAG
        }
    val logSlot = mutableListOf<() -> String>()

    internal var subject = Log(true, TestValues.LOG_TAG, mockLogger)

    @Test
    fun `when log i and enabledLogs=true`() {
        subject.i { LogMessages.CONNECT }

        verify { mockLogger.i(capture(logSlot)) }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
    }

    @Test
    fun `when log w and enabledLogs=true`() {
        subject.w { LogMessages.CONNECT }

        verify { mockLogger.w(capture(logSlot)) }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
    }

    @Test
    fun `when log e and enabledLogs=true`() {
        subject.e { LogMessages.CONNECT }

        verify { mockLogger.e(capture(logSlot)) }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
    }

    @Test
    fun `when log e with throwable and enabledLogs=true`() {
        val givenThrowable = Exception(ErrorTest.MESSAGE)

        subject.e(givenThrowable) { LogMessages.CONNECT }

        verify { mockLogger.e(givenThrowable, capture(logSlot)) }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
    }

    @Test
    fun `when enableLogs=false`() {
        subject = Log(enableLogs = false, TestValues.LOG_TAG)

        assertThat(subject.logger.tag).isEqualTo(TestValues.LOG_TAG)
    }

    @Test
    fun `when withTag()`() {
        val result = subject.withTag(LogTag.API)

        assertThat(result.logger.tag).isEqualTo(LogTag.API)
    }

    @Test
    fun `when getKtorLogger`() {
        val result = subject.ktorLogger

        assertThat(result).isInstanceOf(KtorLogger::class)
    }

    @Test
    fun `when KtorLogger log`() {
        val ktorLogger = KtorLogger(mockLogger)

        ktorLogger.log(LogMessages.CONNECT)

        verify {
            mockLogger.i(capture(logSlot))
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
    }

    @Test
    fun `when okHttpLogger log`() {
        val result = subject.okHttpLogger()
        result.log(LogMessages.CONNECT)

        verify {
            mockLogger.i(capture(logSlot))
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
    }
}
