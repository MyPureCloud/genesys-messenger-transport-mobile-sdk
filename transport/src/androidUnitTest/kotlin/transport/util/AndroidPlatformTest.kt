package com.genesys.cloud.messenger.transport.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import com.genesys.cloud.messenger.transport.utility.TestValues
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

    @Test
    fun `test TokenGenerator`() {
        val result = TokenGenerator.generate()

        assertThat(result, "Android TokenGenerator generated platform UUID").isNotEmpty()
    }

    @Test
    fun `test Vault Keys`() {
        val result = Vault.Keys(
            vaultKey = TestValues.VaultKey,
            tokenKey = TestValues.TokenKey,
            authRefreshTokenKey = TestValues.AuthRefreshTokenKey,
            wasAuthenticated = TestValues.WasAuthenticated,
        )

        result.run {
            assertThat(vaultKey).isEqualTo(TestValues.VaultKey)
            assertThat(tokenKey).isEqualTo(TestValues.TokenKey)
            assertThat(authRefreshTokenKey).isEqualTo(TestValues.AuthRefreshTokenKey)
            assertThat(wasAuthenticated).isEqualTo(TestValues.WasAuthenticated)
        }
    }
}
