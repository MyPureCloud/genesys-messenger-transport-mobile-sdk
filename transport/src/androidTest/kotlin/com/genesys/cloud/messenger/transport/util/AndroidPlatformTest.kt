package com.genesys.cloud.messenger.transport.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import org.junit.Test
import kotlin.test.assertTrue

class AndroidPlatformTest {
    private val subject = Platform()

    @Test
    fun `test randomUUID()`() {
        val result = subject.randomUUID()

        assertThat(result, "Android platform UUID").isNotEmpty()
    }

    @Test
    fun `test epochTime()`() {
        val givenAcceptableRangeOffset = 10

        val result = System.currentTimeMillis()
        assertTrue { subject.epochMillis() in (result - givenAcceptableRangeOffset)..(result + givenAcceptableRangeOffset) }
    }

    @Test
    fun `test getPlatform()`() {
        val expected = "Android ${android.os.Build.VERSION.SDK_INT}"
        val result = subject.platform

        assertThat(result).isEqualTo(expected)
    }
}
