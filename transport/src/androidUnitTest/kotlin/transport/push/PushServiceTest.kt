package transport.push

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.Empty
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.Result
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.push.DEFAULT_PUSH_CONFIG
import com.genesys.cloud.messenger.transport.push.DeviceTokenException
import com.genesys.cloud.messenger.transport.push.DeviceTokenOperation
import com.genesys.cloud.messenger.transport.push.PushConfig
import com.genesys.cloud.messenger.transport.push.PushConfigComparator
import com.genesys.cloud.messenger.transport.push.PushConfigComparator.Diff
import com.genesys.cloud.messenger.transport.push.PushServiceImpl
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.Vault
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.utility.ErrorTest
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
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class PushServiceTest {

    private val mockVault: Vault =
        mockk {
            every { pushConfig } returns DEFAULT_PUSH_CONFIG
            every { pushConfig = any() } just Runs
            every { token } returns TestValues.TOKEN
            every { keys } returns TestValues.vaultKeys
            every { remove(any()) } just Runs
        }
    private val mockApi: WebMessagingApi =
        mockk {
            coEvery { performDeviceTokenOperation(any(), any()) } returns Result.Success(Empty())
        }
    private val mockPlatform: Platform =
        mockk {
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
    fun `when synchronize and diff is NONE`() =
        runTest {
            every { mockPushConfigComparator.compare(any(), any()) } returns Diff.NONE
            val expectedUserConfig = PushTestValues.CONFIG
            val expectedStoredConfig = DEFAULT_PUSH_CONFIG

            subject.synchronize(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)

            verifySequence {
                syncSequence(expectedUserConfig, expectedStoredConfig)
                mockLogger.i(capture(logSlot))
            }
            assertBaseSynchronizeLogsFor(Diff.NONE)
            assertThat(logSlot[2].invoke()).isEqualTo(
                LogMessages.deviceTokenIsInSync(expectedUserConfig)
            )
        }

    @Test
    fun `when synchronize and diff is NO_TOKEN`() =
        runTest {
            every { mockPushConfigComparator.compare(any(), any()) } returns Diff.NO_TOKEN
            val expectedUserConfig = PushTestValues.CONFIG
            val expectedStoredConfig = DEFAULT_PUSH_CONFIG
            val expectedOperation = DeviceTokenOperation.Register

            subject.synchronize(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)

            coVerifySequence {
                syncSequence(expectedUserConfig, expectedStoredConfig)
                mockApi.performDeviceTokenOperation(expectedUserConfig, expectedOperation)
                mockLogger.i(capture(logSlot))
                mockVault.pushConfig = expectedUserConfig
            }
            assertBaseSynchronizeLogsFor(Diff.NO_TOKEN)
            assertThat(logSlot[2].invoke()).isEqualTo(
                LogMessages.deviceTokenWasRegistered(expectedUserConfig)
            )
        }

    @Test
    fun `when synchronize and diff is TOKEN`() =
        runTest {
            every { mockPushConfigComparator.compare(any(), any()) } returns Diff.TOKEN
            val expectedUserConfig = PushTestValues.CONFIG
            val expectedStoredConfig = DEFAULT_PUSH_CONFIG
            val expectedOperation = DeviceTokenOperation.Register

            subject.synchronize(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)

            coVerifySequence {
                syncSequence(expectedUserConfig, expectedStoredConfig)
                mockApi.performDeviceTokenOperation(expectedUserConfig, expectedOperation)
                mockLogger.i(capture(logSlot))
                mockVault.pushConfig = expectedUserConfig
            }
            assertBaseSynchronizeLogsFor(Diff.TOKEN)
            assertThat(logSlot[2].invoke()).isEqualTo(
                LogMessages.deviceTokenWasRegistered(expectedUserConfig)
            )
        }

    @Test
    fun `when synchronize and diff is TOKEN but delete operation resulted in Failure due to ErrorCode DeviceNotFound`() =
        runTest {
            mockResultFailureWith(DeviceTokenOperation.Delete, ErrorCode.DeviceNotFound)
            every { mockPushConfigComparator.compare(any(), any()) } returns Diff.TOKEN
            val expectedUserConfig = PushTestValues.CONFIG
            val expectedStoredConfig = DEFAULT_PUSH_CONFIG
            val expectedOperation = DeviceTokenOperation.Register

            subject.synchronize(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)

            coVerifySequence {
                syncSequence(expectedUserConfig, expectedStoredConfig)
                mockApi.performDeviceTokenOperation(expectedUserConfig, expectedOperation)
                mockLogger.i(capture(logSlot))
                mockVault.pushConfig = expectedUserConfig
            }
            assertBaseSynchronizeLogsFor(Diff.TOKEN)
            assertThat(logSlot[2].invoke()).isEqualTo(
                LogMessages.deviceTokenWasRegistered(expectedUserConfig)
            )
        }

    @Test
    fun `when synchronize and diff is DEVICE_TOKEN`() =
        runTest {
            every { mockPushConfigComparator.compare(any(), any()) } returns Diff.DEVICE_TOKEN
            val expectedUserConfig = PushTestValues.CONFIG
            val expectedStoredConfig = DEFAULT_PUSH_CONFIG
            val expectedOperation = DeviceTokenOperation.Update

            subject.synchronize(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)

            coVerifySequence {
                syncSequence(expectedUserConfig, expectedStoredConfig)
                mockApi.performDeviceTokenOperation(expectedUserConfig, expectedOperation)
                mockLogger.i(capture(logSlot))
                mockVault.pushConfig = expectedUserConfig
            }
            assertBaseSynchronizeLogsFor(Diff.DEVICE_TOKEN)
            assertThat(logSlot[2].invoke()).isEqualTo(
                LogMessages.deviceTokenWasUpdated(expectedUserConfig)
            )
        }

    @Test
    fun `when synchronize and diff is LANGUAGE`() =
        runTest {
            every { mockPushConfigComparator.compare(any(), any()) } returns Diff.LANGUAGE
            val expectedUserConfig = PushTestValues.CONFIG
            val expectedStoredConfig = DEFAULT_PUSH_CONFIG
            val expectedOperation = DeviceTokenOperation.Update

            subject.synchronize(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)

            coVerifySequence {
                syncSequence(expectedUserConfig, expectedStoredConfig)
                mockApi.performDeviceTokenOperation(expectedUserConfig, expectedOperation)
                mockLogger.i(capture(logSlot))
                mockVault.pushConfig = expectedUserConfig
            }
            assertBaseSynchronizeLogsFor(Diff.LANGUAGE)
            assertThat(logSlot[2].invoke()).isEqualTo(
                LogMessages.deviceTokenWasUpdated(expectedUserConfig)
            )
        }

    @Test
    fun `when synchronize and diff is EXPIRED`() =
        runTest {
            every { mockPushConfigComparator.compare(any(), any()) } returns Diff.EXPIRED
            val expectedUserConfig = PushTestValues.CONFIG
            val expectedStoredConfig = DEFAULT_PUSH_CONFIG
            val expectedOperation = DeviceTokenOperation.Update

            subject.synchronize(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)

            coVerifySequence {
                syncSequence(expectedUserConfig, expectedStoredConfig)
                mockApi.performDeviceTokenOperation(expectedUserConfig, expectedOperation)
                mockLogger.i(capture(logSlot))
                mockVault.pushConfig = expectedUserConfig
            }
            assertBaseSynchronizeLogsFor(Diff.EXPIRED)
            assertThat(logSlot[2].invoke()).isEqualTo(
                LogMessages.deviceTokenWasUpdated(expectedUserConfig)
            )
        }

    @Test
    fun `when unregister but device was not registered before`() =
        runTest {

            subject.unregister()

            coVerifySequence {
                mockLogger.i(capture(logSlot))
                mockVault.pushConfig
                mockLogger.i(capture(logSlot))
            }
            assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.UNREGISTERING_DEVICE)
            assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.DEVICE_NOT_REGISTERED)
        }

    @Test
    fun `when unregister and device is registered`() =
        runTest {
            every { mockVault.pushConfig } returns PushTestValues.CONFIG
            val expectedUserConfig = PushTestValues.CONFIG

            subject.unregister()

            coVerifySequence {
                mockLogger.i(capture(logSlot))
                mockVault.pushConfig
                mockApi.performDeviceTokenOperation(expectedUserConfig, DeviceTokenOperation.Delete)
                mockLogger.i(capture(logSlot))
                mockVault.keys
                mockVault.remove(TestValues.vaultKeys.pushConfigKey)
            }
            assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.UNREGISTERING_DEVICE)
            assertThat(logSlot[1].invoke()).isEqualTo(
                LogMessages.deviceTokenWasDeleted(
                    expectedUserConfig
                )
            )
        }

    @Test
    fun `when register() resulted in Failure due to CancellationError`() =
        runTest {
            every { mockPushConfigComparator.compare(any(), any()) } returns Diff.NO_TOKEN
            coEvery {
                mockApi.performDeviceTokenOperation(
                    any(),
                    DeviceTokenOperation.Register
                )
            } returns
                Result.Failure(
                    ErrorCode.CancellationError,
                    ErrorTest.MESSAGE,
                    CancellationException()
                )
            val expectedUserConfig = PushTestValues.CONFIG
            val expectedStoredConfig = DEFAULT_PUSH_CONFIG
            val expectedOperation = DeviceTokenOperation.Register

            assertFailsWith<CancellationException> {
                subject.synchronize(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)
            }

            coVerifySequence {
                syncSequence(expectedUserConfig, expectedStoredConfig)
                mockApi.performDeviceTokenOperation(expectedUserConfig, expectedOperation)
                mockLogger.w(capture(logSlot))
            }
            assertBaseSynchronizeLogsFor(Diff.NO_TOKEN)
            assertThat(logSlot[2].invoke()).isEqualTo(
                LogMessages.cancellationExceptionRequestName("DeviceToken.${DeviceTokenOperation.Register}")
            )
        }

    @Test
    fun `when register() resulted in Failure due to CancellationError but without throwable`() =
        runTest {
            every { mockPushConfigComparator.compare(any(), any()) } returns Diff.NO_TOKEN
            mockResultFailureWith(DeviceTokenOperation.Register, ErrorCode.CancellationError)
            val expectedUserConfig = PushTestValues.CONFIG
            val expectedStoredConfig = DEFAULT_PUSH_CONFIG
            val expectedOperation = DeviceTokenOperation.Register

            assertFailsWith<DeviceTokenException> {
                subject.synchronize(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)
            }

            coVerifySequence {
                syncSequence(expectedUserConfig, expectedStoredConfig)
                mockApi.performDeviceTokenOperation(expectedUserConfig, expectedOperation)
                mockLogger.w(capture(logSlot))
            }
            assertBaseSynchronizeLogsFor(Diff.NO_TOKEN)
            assertThat(logSlot[2].invoke()).isEqualTo(
                LogMessages.cancellationExceptionRequestName("DeviceToken.${DeviceTokenOperation.Register}")
            )
        }

    @Test
    fun `when register() resulted in Failure due to ErrorCode DeviceNotFound`() {
        testRegistrationWithError(ErrorCode.DeviceNotFound)
    }

    @Test
    fun `when register() resulted in Failure due to any ErrorCode`() {
        testRegistrationWithError(ErrorCode.DeviceRegistrationFailure)
    }

    @Test
    fun `when register() resulted in Failure due to ErrorCode DeviceAlreadyRegistered`() =
        runTest {
            every { mockPushConfigComparator.compare(any(), any()) } returns Diff.NO_TOKEN
            mockResultFailureWith(DeviceTokenOperation.Register, ErrorCode.DeviceAlreadyRegistered)
            val expectedUserConfig = PushTestValues.CONFIG
            val expectedStoredConfig = DEFAULT_PUSH_CONFIG
            val expectedOperation1 = DeviceTokenOperation.Register
            val expectedOperation2 = DeviceTokenOperation.Update

            subject.synchronize(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)

            coVerifySequence {
                syncSequence(expectedUserConfig, expectedStoredConfig)
                mockApi.performDeviceTokenOperation(expectedUserConfig, expectedOperation1)
                mockLogger.i(capture(logSlot))
                mockVault.pushConfig = expectedUserConfig
                mockApi.performDeviceTokenOperation(expectedUserConfig, expectedOperation2)
                mockLogger.i(capture(logSlot))
                mockVault.pushConfig = expectedUserConfig
            }
            assertBaseSynchronizeLogsFor(Diff.NO_TOKEN)
            assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.DEVICE_ALREADY_REGISTERED)
            assertThat(logSlot[3].invoke()).isEqualTo(LogMessages.deviceTokenWasUpdated(expectedUserConfig))
        }

    private fun testRegistrationWithError(errorCode: ErrorCode) =
        runTest {
            every { mockPushConfigComparator.compare(any(), any()) } returns Diff.NO_TOKEN
            mockResultFailureWith(DeviceTokenOperation.Register, errorCode)
            val expectedUserConfig = PushTestValues.CONFIG
            val expectedStoredConfig = DEFAULT_PUSH_CONFIG
            val expectedOperation = DeviceTokenOperation.Register

            assertFailsWith<DeviceTokenException> {
                subject.synchronize(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)
            }

            coVerifySequence {
                syncSequence(expectedUserConfig, expectedStoredConfig)
                mockApi.performDeviceTokenOperation(expectedUserConfig, expectedOperation)
                mockLogger.e(capture(logSlot))
            }
            assertBaseSynchronizeLogsFor(Diff.NO_TOKEN)
            assertThat(logSlot[2].invoke()).isEqualTo(
                LogMessages.failedToSynchronizeDeviceToken(PushTestValues.CONFIG, errorCode)
            )
        }

    @Test
    fun `when register() resulted in Failure due to identity resolution error`() {
        testRegistrationWithError(ErrorCode.DeviceRegistrationFailure)
    }

    @Test
    fun `when update() resulted in Failure due to ErrorCode DeviceNotFound`() =
        runTest {
            every { mockPushConfigComparator.compare(any(), any()) } returns Diff.LANGUAGE
            mockResultFailureWith(DeviceTokenOperation.Update, ErrorCode.DeviceNotFound)
            mockResultSuccess(DeviceTokenOperation.Register)
            val expectedUserConfig = PushTestValues.CONFIG
            val expectedStoredConfig = DEFAULT_PUSH_CONFIG
            val expectedOperation = DeviceTokenOperation.Update

            subject.synchronize(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)

            coVerifySequence {
                syncSequence(expectedUserConfig, expectedStoredConfig)
                mockApi.performDeviceTokenOperation(expectedUserConfig, expectedOperation)
                mockLogger.i(capture(logSlot))
                mockApi.performDeviceTokenOperation(expectedUserConfig, DeviceTokenOperation.Register)
                mockLogger.i(capture(logSlot))
                mockVault.pushConfig = expectedUserConfig
            }
            assertBaseSynchronizeLogsFor(Diff.LANGUAGE)
            assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.DEVICE_NOT_REGISTERED)
            assertThat(logSlot[3].invoke()).isEqualTo(
                LogMessages.deviceTokenWasRegistered(
                    expectedUserConfig
                )
            )
        }

    @Test
    fun `when unregister() resulted in Failure due to ErrorCode DeviceNotFound`() =
        runTest {
            mockResultFailureWith(DeviceTokenOperation.Delete, ErrorCode.DeviceNotFound)
            every { mockVault.pushConfig } returns PushTestValues.CONFIG
            val expectedUserConfig = PushTestValues.CONFIG

            subject.unregister()

            coVerifySequence {
                mockLogger.i(capture(logSlot))
                mockVault.pushConfig
                mockApi.performDeviceTokenOperation(expectedUserConfig, DeviceTokenOperation.Delete)
                mockLogger.i(capture(logSlot))
                mockVault.keys
                mockVault.remove(TestValues.vaultKeys.pushConfigKey)
            }
            assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.UNREGISTERING_DEVICE)
            assertThat(logSlot[1].invoke()).isEqualTo(
                LogMessages.deviceTokenWasDeleted(
                    expectedUserConfig
                )
            )
        }

    private fun mockResultFailureWith(
        operation: DeviceTokenOperation,
        errorCode: ErrorCode
    ) {
        coEvery {
            mockApi.performDeviceTokenOperation(any(), operation)
        } returns Result.Failure(errorCode)
    }

    private fun mockResultSuccess(operation: DeviceTokenOperation) {
        coEvery {
            mockApi.performDeviceTokenOperation(any(), operation)
        } returns Result.Success(Empty())
    }

    private fun assertBaseSynchronizeLogsFor(diff: Diff) {
        assertThat(logSlot[0].invoke()).isEqualTo(
            LogMessages.synchronizingPush(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)
        )
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.pushDiff(diff))
    }

    private fun MockKVerificationScope.syncSequence(
        expectedUserConfig: PushConfig,
        expectedStoredConfig: PushConfig
    ) {
        mockLogger.i(capture(logSlot))
        mockVault.pushConfig
        mockVault.token
        mockPlatform.preferredLanguage()
        mockPlatform.epochMillis()
        mockPlatform.os
        mockPushConfigComparator.compare(expectedUserConfig, expectedStoredConfig)
        mockLogger.i(capture(logSlot))
    }
}
