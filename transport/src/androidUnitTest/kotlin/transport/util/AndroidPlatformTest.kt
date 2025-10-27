package transport.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import com.genesys.cloud.messenger.transport.util.AUTH_REFRESH_TOKEN_KEY
import com.genesys.cloud.messenger.transport.util.PUSH_CONFIG_KEY
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.TOKEN_KEY
import com.genesys.cloud.messenger.transport.util.TokenGenerator
import com.genesys.cloud.messenger.transport.util.VAULT_KEY
import com.genesys.cloud.messenger.transport.util.Vault
import com.genesys.cloud.messenger.transport.util.WAS_AUTHENTICATED
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
            vaultKey = VAULT_KEY,
            tokenKey = TOKEN_KEY,
            authRefreshTokenKey = AUTH_REFRESH_TOKEN_KEY,
            wasAuthenticated = WAS_AUTHENTICATED,
            pushConfigKey = PUSH_CONFIG_KEY
        )

        result.run {
            assertThat(vaultKey).isEqualTo(VAULT_KEY)
            assertThat(tokenKey).isEqualTo(TOKEN_KEY)
            assertThat(authRefreshTokenKey).isEqualTo(AUTH_REFRESH_TOKEN_KEY)
            assertThat(wasAuthenticated).isEqualTo(WAS_AUTHENTICATED)
            assertThat(pushConfigKey).isEqualTo(PUSH_CONFIG_KEY)
        }
    }

    @Test
    fun `test os`() {
        val expected = "Android"

        val result = subject.os

        assertThat(result).isEqualTo(expected)
    }
}
