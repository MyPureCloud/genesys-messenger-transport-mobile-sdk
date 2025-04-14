package transport.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.TokenGenerator
import com.genesys.cloud.messenger.transport.util.Vault
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
            vaultKey = TestValues.VAULT_KEY,
            tokenKey = TestValues.TOKEN_KEY,
            authRefreshTokenKey = TestValues.AUTH_REFRESH_TOKEN_KEY,
            wasAuthenticated = TestValues.WAS_AUTHENTICATED,
        )

        result.run {
            assertThat(vaultKey).isEqualTo(TestValues.VAULT_KEY)
            assertThat(tokenKey).isEqualTo(TestValues.TOKEN_KEY)
            assertThat(authRefreshTokenKey).isEqualTo(TestValues.AUTH_REFRESH_TOKEN_KEY)
            assertThat(wasAuthenticated).isEqualTo(TestValues.WAS_AUTHENTICATED)
        }
    }
}
