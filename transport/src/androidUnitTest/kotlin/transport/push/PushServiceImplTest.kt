package transport.push

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.Empty
import com.genesys.cloud.messenger.transport.core.Result
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.push.DEFAULT_PUSH_CONFIG
import com.genesys.cloud.messenger.transport.push.PushConfig
import com.genesys.cloud.messenger.transport.push.PushConfigComparator
import com.genesys.cloud.messenger.transport.push.PushConfigComparator.Diff
import com.genesys.cloud.messenger.transport.push.PushServiceImpl
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.Vault
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.utility.PushTestValues
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.mockk.MockKVerificationScope
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class PushServiceImplTest {

    private val mockVault: Vault = mockk {
        every { pushConfig } returns DEFAULT_PUSH_CONFIG
        every { pushConfig = any() } just Runs
        every { token } returns TestValues.Token
    }
    private val mockApi: WebMessagingApi = mockk {
        coEvery { registerDeviceToken(any()) } returns Result.Success(Empty())
        coEvery { updateDeviceToken(any()) } returns Result.Success(Empty())
        coEvery { deleteDeviceToken(any()) } returns Result.Success(Empty())
    }
    private val mockPlatform: Platform = mockk {
        every { preferredLanguage() } returns TestValues.PREFERRED_LANGUAGE
        every { epochMillis() } returns TestValues.PUSH_SYNC_TIMESTAMP
        every { os } returns TestValues.DEVICE_TYPE
    }
    private val mockPushConfigComparator: PushConfigComparator = mockk()
    private val mockLogger: Log = mockk(relaxed = true)
    private val logSlot = mutableListOf<() -> String>()

    private val subject: PushServiceImpl =
        PushServiceImpl(mockVault, mockApi, mockPlatform, mockPushConfigComparator, mockLogger)

    @Test
    fun `when synchronize and diff is NONE`() {
        every { mockPushConfigComparator.compare(any(), any()) } returns Diff.NONE
        val expectedUserConfig = PushTestValues.CONFIG
        val expectedStoredConfig = DEFAULT_PUSH_CONFIG

        runBlocking {
            subject.synchronize(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)
        }

        verifySequence {
            syncSequence(expectedUserConfig, expectedStoredConfig)
            mockLogger.i(capture(logSlot))
        }
        assertThat(logSlot[0].invoke()).isEqualTo(
            LogMessages.synchronizingPush(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)
        )
        assertThat(logSlot[1].invoke()).isEqualTo(
            LogMessages.deviceTokenIsInSync(expectedUserConfig)
        )
    }

    @Test
    fun `when synchronize and diff is NO_TOKEN`() {
        every { mockPushConfigComparator.compare(any(), any()) } returns Diff.NO_TOKEN
        val expectedUserConfig = PushTestValues.CONFIG
        val expectedStoredConfig = DEFAULT_PUSH_CONFIG

        runBlocking {
            subject.synchronize(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)
        }

        coVerifySequence {
            syncSequence(expectedUserConfig, expectedStoredConfig)
            mockApi.registerDeviceToken(expectedUserConfig)
            mockLogger.i(capture(logSlot))
            mockVault.pushConfig = expectedUserConfig
        }
        assertThat(logSlot[0].invoke()).isEqualTo(
            LogMessages.synchronizingPush(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)
        )
        assertThat(logSlot[1].invoke()).isEqualTo(
            LogMessages.deviceTokenWasRegistered(expectedUserConfig)
        )
    }

    @Test
    fun `when synchronize and diff is TOKEN`() {
        every { mockPushConfigComparator.compare(any(), any()) } returns Diff.TOKEN
        val expectedUserConfig = PushTestValues.CONFIG
        val expectedStoredConfig = DEFAULT_PUSH_CONFIG

        runBlocking {
            subject.synchronize(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)
        }

        coVerifySequence {
            syncSequence(expectedUserConfig, expectedStoredConfig)
            mockApi.deleteDeviceToken(expectedUserConfig)
            mockLogger.i(capture(logSlot))
            mockApi.registerDeviceToken(expectedUserConfig)
            mockLogger.i(capture(logSlot))
            mockVault.pushConfig = expectedUserConfig
        }
        assertThat(logSlot[0].invoke()).isEqualTo(
            LogMessages.synchronizingPush(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)
        )
        assertThat(logSlot[1].invoke()).isEqualTo(
            LogMessages.deviceTokenWasDeleted(expectedUserConfig)
        )
        assertThat(logSlot[2].invoke()).isEqualTo(
            LogMessages.deviceTokenWasRegistered(expectedUserConfig)
        )
    }

    @Test
    fun `when synchronize and diff is DEVICE_TOKEN`() {
        every { mockPushConfigComparator.compare(any(), any()) } returns Diff.DEVICE_TOKEN
        val expectedUserConfig = PushTestValues.CONFIG
        val expectedStoredConfig = DEFAULT_PUSH_CONFIG

        runBlocking {
            subject.synchronize(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)
        }

        coVerifySequence {
            syncSequence(expectedUserConfig, expectedStoredConfig)
            mockApi.updateDeviceToken(expectedUserConfig)
            mockLogger.i(capture(logSlot))
            mockVault.pushConfig = expectedUserConfig
        }
        assertThat(logSlot[0].invoke()).isEqualTo(
            LogMessages.synchronizingPush(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)
        )
        assertThat(logSlot[1].invoke()).isEqualTo(
            LogMessages.deviceTokenWasUpdated(expectedUserConfig)
        )
    }

    @Test
    fun `when synchronize and diff is LANGUAGE`() {
        every { mockPushConfigComparator.compare(any(), any()) } returns Diff.LANGUAGE
        val expectedUserConfig = PushTestValues.CONFIG
        val expectedStoredConfig = DEFAULT_PUSH_CONFIG

        runBlocking {
            subject.synchronize(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)
        }

        coVerifySequence {
            syncSequence(expectedUserConfig, expectedStoredConfig)
            mockApi.updateDeviceToken(expectedUserConfig)
            mockLogger.i(capture(logSlot))
            mockVault.pushConfig = expectedUserConfig
        }
        assertThat(logSlot[0].invoke()).isEqualTo(
            LogMessages.synchronizingPush(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)
        )
        assertThat(logSlot[1].invoke()).isEqualTo(
            LogMessages.deviceTokenWasUpdated(expectedUserConfig)
        )
    }

    @Test
    fun `when synchronize and diff is EXPIRED`() {
        every { mockPushConfigComparator.compare(any(), any()) } returns Diff.EXPIRED
        val expectedUserConfig = PushTestValues.CONFIG
        val expectedStoredConfig = DEFAULT_PUSH_CONFIG

        runBlocking {
            subject.synchronize(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)
        }

        coVerifySequence {
            syncSequence(expectedUserConfig, expectedStoredConfig)
            mockApi.updateDeviceToken(expectedUserConfig)
            mockLogger.i(capture(logSlot))
            mockVault.pushConfig = expectedUserConfig
        }
        assertThat(logSlot[0].invoke()).isEqualTo(
            LogMessages.synchronizingPush(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)
        )
        assertThat(logSlot[1].invoke()).isEqualTo(
            LogMessages.deviceTokenWasUpdated(expectedUserConfig)
        )
    }

    private fun MockKVerificationScope.syncSequence(expectedUserConfig: PushConfig, expectedStoredConfig: PushConfig) {
        mockLogger.i(capture(logSlot))
        mockVault.pushConfig
        mockVault.token
        mockPlatform.preferredLanguage()
        mockPlatform.epochMillis()
        mockPlatform.os
        mockPushConfigComparator.compare(expectedUserConfig, expectedStoredConfig)
    }
}
