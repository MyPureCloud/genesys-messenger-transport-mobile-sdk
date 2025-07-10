package com.genesys.cloud.messenger.transport.push

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.ErrorMessage
import com.genesys.cloud.messenger.transport.push.PushConfigComparator.Diff
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.utility.PushTestValues
import com.genesys.cloud.messenger.transport.utility.TestValues
import kotlin.test.Test
import kotlin.test.assertFailsWith

class PushConfigComparatorTest {

    private val subject: PushConfigComparator = PushConfigComparatorImpl()

    @Test
    fun `when userConfig deviceToken is empty string`() {
        val givenUserConfig = PushTestValues.CONFIG.copy(deviceToken = "")
        val givenStoredConfig = PushTestValues.CONFIG
        val expectedExceptionMessage = ErrorMessage.INVALID_DEVICE_TOKEN

        val exception = assertFailsWith<IllegalArgumentException> {
            subject.compare(givenUserConfig, givenStoredConfig)
        }

        assertThat(exception.message).isEqualTo(expectedExceptionMessage)
    }

    @Test
    fun `when userConfig pushProvider is null`() {
        val givenUserConfig = PushTestValues.CONFIG.copy(pushProvider = null)
        val givenStoredConfig = PushTestValues.CONFIG
        val expectedExceptionMessage = ErrorMessage.INVALID_PUSH_PROVIDER

        val exception = assertFailsWith<IllegalArgumentException> {
            subject.compare(givenUserConfig, givenStoredConfig)
        }

        assertThat(exception.message).isEqualTo(expectedExceptionMessage)
    }

    @Test
    fun `when compare 2 similar pushConfig objects`() {
        val givenUserConfig = PushTestValues.CONFIG
        val givenStoredConfig = PushTestValues.CONFIG

        val result = subject.compare(givenUserConfig, givenStoredConfig)

        assertThat(result).isEqualTo(Diff.NONE)
    }

    @Test
    fun `when storedConfig has unknown token`() {
        val givenUserConfig = PushTestValues.CONFIG
        val givenStoredConfig = DEFAULT_PUSH_CONFIG

        val result = subject.compare(givenUserConfig, givenStoredConfig)

        assertThat(result).isEqualTo(Diff.NO_TOKEN)
    }

    @Test
    fun `when userConfig token is different from storedConfig token`() {
        val givenUserConfig = PushTestValues.CONFIG
        val givenStoredConfig = PushTestValues.CONFIG.copy(
            token = TestValues.DEFAULT_STRING,
            deviceToken = TestValues.DEFAULT_STRING,
            preferredLanguage = TestValues.DEFAULT_STRING,
        )

        val result = subject.compare(givenUserConfig, givenStoredConfig)

        assertThat(result).isEqualTo(Diff.TOKEN)
    }

    @Test
    fun `when userConfig deviceToken is different from storedConfig deviceToken`() {
        val givenUserConfig = PushTestValues.CONFIG
        val givenStoredConfig = PushTestValues.CONFIG.copy(
            deviceToken = TestValues.DEFAULT_STRING,
            preferredLanguage = TestValues.DEFAULT_STRING,
        )

        val result = subject.compare(givenUserConfig, givenStoredConfig)

        assertThat(result).isEqualTo(Diff.DEVICE_TOKEN)
    }

    @Test
    fun `when userConfig preferredLanguage is different from storedConfig preferredLanguage`() {
        val givenUserConfig = PushTestValues.CONFIG
        val givenStoredConfig =
            PushTestValues.CONFIG.copy(preferredLanguage = TestValues.DEFAULT_STRING)

        val result = subject.compare(givenUserConfig, givenStoredConfig)

        assertThat(result).isEqualTo(Diff.LANGUAGE)
    }

    @Test
    fun `when storedConfig lastSyncTimestamp exactly as DEVICE_TOKEN_EXPIRATION_IN_MILLISECONDS`() {
        val currentTimestamp = Platform().epochMillis()
        val givenStoredConfigTimestamp = currentTimestamp - DEVICE_TOKEN_EXPIRATION_IN_MILLISECONDS
        val givenUserConfig = PushTestValues.CONFIG.copy(lastSyncTimestamp = currentTimestamp)
        val givenStoredConfig =
            PushTestValues.CONFIG.copy(lastSyncTimestamp = givenStoredConfigTimestamp)

        val result = subject.compare(givenUserConfig, givenStoredConfig)

        assertThat(result).isEqualTo(Diff.EXPIRED)
    }

    @Test
    fun `when storedConfig lastSyncTimestamp higher then DEVICE_TOKEN_EXPIRATION_IN_MILLISECONDS`() {
        val currentTimestamp = Platform().epochMillis()
        val givenStoredConfigTimestamp = currentTimestamp - DEVICE_TOKEN_EXPIRATION_IN_MILLISECONDS - 1
        val givenUserConfig = PushTestValues.CONFIG.copy(lastSyncTimestamp = currentTimestamp)
        val givenStoredConfig =
            PushTestValues.CONFIG.copy(lastSyncTimestamp = givenStoredConfigTimestamp)

        val result = subject.compare(givenUserConfig, givenStoredConfig)

        assertThat(result).isEqualTo(Diff.EXPIRED)
    }

    @Test
    fun `when storedConfig lastSyncTimestamp lower then DEVICE_TOKEN_EXPIRATION_IN_MILLISECONDS`() {
        val currentTimestamp = Platform().epochMillis()
        val givenStoredConfigTimestamp = currentTimestamp - DEVICE_TOKEN_EXPIRATION_IN_MILLISECONDS + 1
        val givenUserConfig = PushTestValues.CONFIG.copy(lastSyncTimestamp = currentTimestamp)
        val givenStoredConfig =
            PushTestValues.CONFIG.copy(lastSyncTimestamp = givenStoredConfigTimestamp)

        val result = subject.compare(givenUserConfig, givenStoredConfig)

        assertThat(result).isEqualTo(Diff.NONE)
    }

    @Test
    fun `when userConfig and storedConfig differ in deviceType and pushProvider`() {
        val givenUserConfig = PushTestValues.CONFIG
        val givenStoredConfig = PushTestValues.CONFIG.copy(
            deviceType = TestValues.DEFAULT_STRING,
            pushProvider = PushProvider.FCM
        )

        val result = subject.compare(givenUserConfig, givenStoredConfig)

        assertThat(result).isEqualTo(Diff.NONE)
    }
}
