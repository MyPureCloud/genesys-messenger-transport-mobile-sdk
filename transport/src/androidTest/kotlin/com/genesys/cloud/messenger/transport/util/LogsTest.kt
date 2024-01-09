package com.genesys.cloud.messenger.transport.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.genesys.cloud.messenger.transport.util.logs.KermitKtorLogger
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogTag
import com.genesys.cloud.messenger.transport.util.logs.okHttpLogger
import com.genesys.cloud.messenger.transport.utility.ErrorTest
import com.genesys.cloud.messenger.transport.utility.LogMessages
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test

class LogsTest {
    private val mockKermit: Logger = mockk(relaxed = true) {
        every { tag } returns LogMessages.LogTag
    }

    internal var subject = Log(true, LogMessages.LogTag, mockKermit)

    @Test
    fun `when log i and enabledLogs=true`() {
        subject.i { LogMessages.Connect }

        verify {
            mockKermit.log(Severity.Info, LogMessages.LogTag, null, LogMessages.Connect)
        }
    }

    @Test
    fun `when log w and enabledLogs=true`() {
        subject.w { LogMessages.Connect }

        verify {
            mockKermit.log(Severity.Warn, LogMessages.LogTag, null, LogMessages.Connect)
        }
    }

    @Test
    fun `when log e and enabledLogs=true`() {
        subject.e { LogMessages.Connect }

        verify {
            mockKermit.log(Severity.Error, LogMessages.LogTag, null, LogMessages.Connect)
        }
    }

    @Test
    fun `when log e with throwable and enabledLogs=true`() {
        val givenThrowable = Exception(ErrorTest.Message)

        subject.e(givenThrowable) { LogMessages.Connect }

        verify {
            mockKermit.log(Severity.Error, LogMessages.LogTag, givenThrowable, LogMessages.Connect)
        }
    }

    @Test
    fun `when enableLogs=false`() {
        subject = Log(enableLogs = false, LogMessages.LogTag)

        assertThat(subject.kermit.tag).isEqualTo(LogMessages.LogTag)
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

        kermitKtorLogger.log(LogMessages.Connect)

        verify {
            mockKermit.log(Severity.Info, "", null ,capture(slot))
        }
        assertThat(slot.captured).isEqualTo(LogMessages.Connect)
    }

    @Test
    fun `when okHttpLogger log`() {
        val slot = slot<String>()

        val result = subject.okHttpLogger()
        result.log( LogMessages.Connect)

        verify {
            mockKermit.i(capture(slot))
        }
        assertThat(slot.captured).isEqualTo(LogMessages.Connect)
    }
}
