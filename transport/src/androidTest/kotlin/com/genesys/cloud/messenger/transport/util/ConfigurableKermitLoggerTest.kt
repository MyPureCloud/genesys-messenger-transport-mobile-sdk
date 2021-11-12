package com.genesys.cloud.messenger.transport.util

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.genesys.cloud.messenger.transport.util.logs.ConfigurableKermitLogger
import io.mockk.mockk
import kotlin.test.Test

class ConfigurableKermitLoggerTest {

    private val delegate: Logger = mockk()

    @Test
    fun `It should always evaluate severity as loggable when enabled`() {
        val subject = ConfigurableKermitLogger(
            enabled = true,
            delegate = delegate
        )
        val allSeverities = Severity.values()

        for (severity in allSeverities) {
            val actual = subject.isLoggable(severity)

            assertThat(actual, "severity $severity").isTrue()
        }
    }

    @Test
    fun `It should always evaluate severity as not loggable when disabled`() {
        val subject = ConfigurableKermitLogger(
            enabled = false,
            delegate = delegate
        )
        val allSeverities = Severity.values()

        for (severity in allSeverities) {
            val actual = subject.isLoggable(severity)

            assertThat(actual, "severity $severity").isFalse()
        }
    }
}
