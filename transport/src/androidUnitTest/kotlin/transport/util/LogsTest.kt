package transport.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.genesys.cloud.messenger.transport.util.logs.KermitKtorLogger
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.util.logs.LogTag
import com.genesys.cloud.messenger.transport.util.logs.okHttpLogger
import com.genesys.cloud.messenger.transport.utility.ErrorTest
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test

class LogsTest {
    private val mockKermit: Logger = mockk(relaxed = true) {
        every { tag } returns TestValues.LogTag
    }

    internal var subject = Log(true, TestValues.LogTag, mockKermit)

    @Test
    fun `when log i and enabledLogs=true`() {
        subject.i { LogMessages.CONNECT }

        verify {
            mockKermit.log(Severity.Info, TestValues.LogTag, null, LogMessages.CONNECT)
        }
    }

    @Test
    fun `when log w and enabledLogs=true`() {
        subject.w { LogMessages.CONNECT }

        verify {
            mockKermit.log(Severity.Warn, TestValues.LogTag, null, LogMessages.CONNECT)
        }
    }

    @Test
    fun `when log e and enabledLogs=true`() {
        subject.e { LogMessages.CONNECT }

        verify {
            mockKermit.log(Severity.Error, TestValues.LogTag, null, LogMessages.CONNECT)
        }
    }

    @Test
    fun `when log e with throwable and enabledLogs=true`() {
        val givenThrowable = Exception(ErrorTest.Message)

        subject.e(givenThrowable) { LogMessages.CONNECT }

        verify {
            mockKermit.log(Severity.Error, TestValues.LogTag, givenThrowable, LogMessages.CONNECT)
        }
    }

    @Test
    fun `when enableLogs=false`() {
        subject = Log(enableLogs = false, TestValues.LogTag)

        assertThat(subject.kermit.tag).isEqualTo(TestValues.LogTag)
        assertThat(subject.kermit.config.minSeverity).isEqualTo(Severity.Assert)
    }

    @Test
    fun `when withTag()`() {
        val result = subject.withTag(LogTag.API)

        assertThat(result.kermit.tag).isEqualTo(LogTag.API)
    }

    @Test
    fun `when getKtorLogger`() {
        val result = subject.ktorLogger

        assertThat(result).isInstanceOf(KermitKtorLogger::class)
    }

    @Test
    fun `when KermitKtorLogger log`() {
        val slot = slot<String>()
        val mockKermit: Logger = mockk(relaxed = true)
        val kermitKtorLogger = KermitKtorLogger(mockKermit)

        kermitKtorLogger.log(LogMessages.CONNECT)

        verify {
            mockKermit.log(Severity.Info, "", null, capture(slot))
        }
        assertThat(slot.captured).isEqualTo(LogMessages.CONNECT)
    }

    @Test
    fun `when okHttpLogger log`() {
        val slot = slot<String>()

        val result = subject.okHttpLogger()
        result.log(LogMessages.CONNECT)

        verify {
            mockKermit.i(capture(slot))
        }
        assertThat(slot.captured).isEqualTo(LogMessages.CONNECT)
    }
}
