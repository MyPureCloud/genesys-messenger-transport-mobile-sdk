package com.genesys.cloud.messenger.transport.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.auth.NO_REFRESH_TOKEN
import com.genesys.cloud.messenger.transport.push.DEFAULT_PUSH_CONFIG
import com.genesys.cloud.messenger.transport.push.PushConfig
import com.genesys.cloud.messenger.transport.push.PushProvider
import com.genesys.cloud.messenger.transport.utility.AuthTest
import com.genesys.cloud.messenger.transport.utility.TestValues
import kotlin.test.Test

class VaultTest {

    val subject = ConcreteVault()

    @Test
    fun `when getToken`() {
        val result = subject.token

        assertThat(result).isNotEmpty()
    }

    @Test
    fun `when token is removed and then getToken`() {
        subject.remove(TOKEN_KEY)
        val result = subject.token

        assertThat(result).isNotEmpty()
    }

    @Test
    fun `when getAuthToken but it was never set before`() {
        val result = subject.authRefreshToken

        assertThat(result).isEqualTo(NO_REFRESH_TOKEN)
    }

    @Test
    fun `when getAuthToken after it was set`() {
        subject.authRefreshToken = AuthTest.JwtToken
        val result = subject.authRefreshToken

        assertThat(result).isEqualTo(AuthTest.JwtToken)
    }

    @Test
    fun `when authToken is removed and then getAuthToken`() {
        subject.remove(AUTH_REFRESH_TOKEN_KEY)
        val result = subject.authRefreshToken

        assertThat(result).isEqualTo(NO_REFRESH_TOKEN)
    }

    @Test
    fun `when getWasAuthenticated but it was never set before`() {
        val result = subject.wasAuthenticated

        assertThat(result).isFalse()
    }

    @Test
    fun `when getWasAuthenticated after it was set`() {
        subject.wasAuthenticated = true
        val result = subject.wasAuthenticated

        assertThat(result).isTrue()
    }

    @Test
    fun `when wasAuthenticated is removed and then getWasAuthenticated`() {
        subject.remove(WAS_AUTHENTICATED)
        val result = subject.wasAuthenticated

        assertThat(result).isFalse()
    }

    @Test
    fun `when getPushConfig but it was never set before`() {
        val result = subject.pushConfig

        assertThat(result).isEqualTo(DEFAULT_PUSH_CONFIG)
    }

    @Test
    fun `when getPushConfig after it was set`() {
        val givenPushConfig = PushConfig(
            token = TestValues.Token,
            deviceToken = TestValues.DEVICE_TOKEN,
            preferredLanguage = TestValues.PREFERRED_LANGUAGE,
            lastSyncTimestamp = TestValues.PUSH_SYNC_TIMESTAMP,
            deviceType = TestValues.DEVICE_TYPE,
            pushProvider = PushProvider.APNS,
        )

        subject.pushConfig = givenPushConfig
        val result = subject.pushConfig

        assertThat(result).isEqualTo(givenPushConfig)
    }

    @Test
    fun `when pushConfig is removed and then getPushConfig`() {
        subject.remove(PUSH_CONFIG_KEY)
        val result = subject.pushConfig

        assertThat(result).isEqualTo(DEFAULT_PUSH_CONFIG)
    }
}
