package com.genesys.cloud.messenger.transport

import assertk.assertThat
import assertk.assertions.isNotEmpty
import com.genesys.cloud.messenger.transport.util.Platform
import org.junit.Test
import kotlin.test.assertTrue

class AndroidPlatformTest {

    @Test
    fun testRandomUUID() {
        val subject = Platform()

        val actual = subject.randomUUID()

        assertThat(actual, "Android platform UUID").isNotEmpty()
    }

    @Test
    fun testEpochTime() {
        val givenAcceptableRangeOffset = 10
        val subject = Platform()

        val actual = System.currentTimeMillis()
        assertTrue { subject.epochMillis() in (actual - givenAcceptableRangeOffset)..(actual + givenAcceptableRangeOffset) }
    }
}
