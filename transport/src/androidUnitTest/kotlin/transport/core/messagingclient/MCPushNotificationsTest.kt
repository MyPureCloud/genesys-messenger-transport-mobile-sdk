package transport.core.messagingclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.messagingclient.BaseMessagingClientTest
import com.genesys.cloud.messenger.transport.shyrka.receive.Apps
import com.genesys.cloud.messenger.transport.shyrka.receive.Conversations
import com.genesys.cloud.messenger.transport.shyrka.receive.Conversations.Notifications.NotificationContentType
import com.genesys.cloud.messenger.transport.shyrka.receive.createConversationsVOForTesting
import com.genesys.cloud.messenger.transport.shyrka.receive.createDeploymentConfigForTesting
import com.genesys.cloud.messenger.transport.shyrka.receive.createMessengerVOForTesting
import com.genesys.cloud.messenger.transport.util.Request
import com.genesys.cloud.messenger.transport.util.UNKNOWN
import com.genesys.cloud.messenger.transport.util.fromConnectedToConfigured
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.utility.PushTestValues
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verifySequence
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class MCPushNotificationsTest : BaseMessagingClientTest() {

    private val dispatcher: CoroutineDispatcher = Dispatchers.Unconfined

    @ExperimentalCoroutinesApi
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(dispatcher)
    }

    @ExperimentalCoroutinesApi
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when connect() but notifications are disabled in DeploymentConfig`() {

        subject.connect()

        verifySequence {
            connectSequence()
        }
        coVerify(exactly = 0) { mockPushService.synchronize(any(), any()) }
    }

    @Test
    fun `when connect() and notifications are enabled in DeploymentConfig, but deviceToken is UNKNOWN`() {
        every { mockVault.pushConfig } returns PushTestValues.CONFIG.copy(deviceToken = UNKNOWN)
        every { mockDeploymentConfig.get() } returns createDeploymentConfigForTesting(
            messenger = createMessengerVOForTesting(
                apps = Apps(
                    conversations = createConversationsVOForTesting(
                        notifications = Conversations.Notifications(
                            enabled = true,
                            notificationContentType = NotificationContentType.IncludeMessagesContent
                        )
                    )
                )
            )
        )

        subject.connect()

        verifySequence {
            fromIdleToConnectedSequence()
            configureSequence(false)
            mockLogger.i(capture(logSlot))
            mockVault.pushConfig
            mockLogger.i(capture(logSlot))
            mockStateChangedListener(fromConnectedToConfigured)
        }
        coVerify(exactly = 0) { mockPushService.synchronize(any(), any()) }
        assertLogsForPushConfig()
        assertThat(logSlot[3].invoke()).isEqualTo(LogMessages.NO_DEVICE_TOKEN_OR_PUSH_PROVIDER)
    }

    @Test
    fun `when connect() and notifications are enabled in DeploymentConfig, but PushProvider is null`() {
        every { mockVault.pushConfig } returns PushTestValues.CONFIG.copy(pushProvider = null)
        every { mockDeploymentConfig.get() } returns createDeploymentConfigForTesting(
            messenger = createMessengerVOForTesting(
                apps = Apps(
                    conversations = createConversationsVOForTesting(
                        notifications = Conversations.Notifications(
                            enabled = true,
                            notificationContentType = NotificationContentType.IncludeMessagesContent
                        )
                    )
                )
            )
        )

        subject.connect()

        verifySequence {
            fromIdleToConnectedSequence()
            configureSequence(false)
            mockLogger.i(capture(logSlot))
            mockVault.pushConfig
            mockLogger.i(capture(logSlot))
            mockStateChangedListener(fromConnectedToConfigured)
        }
        coVerify(exactly = 0) { mockPushService.synchronize(any(), any()) }
        assertLogsForPushConfig()
        assertThat(logSlot[3].invoke()).isEqualTo(LogMessages.NO_DEVICE_TOKEN_OR_PUSH_PROVIDER)
    }

    @Test
    fun `when connect() and notifications are enabled in DeploymentConfig, and PushConfig is valid`() {
        every { mockVault.pushConfig } returns PushTestValues.CONFIG
        every { mockDeploymentConfig.get() } returns createDeploymentConfigForTesting(
            messenger = createMessengerVOForTesting(
                apps = Apps(
                    conversations = createConversationsVOForTesting(
                        notifications = Conversations.Notifications(
                            enabled = true,
                            notificationContentType = NotificationContentType.IncludeMessagesContent
                        )
                    )
                )
            )
        )
        val expectedDeviceToken = TestValues.DEVICE_TOKEN
        val expectedPushProvider = TestValues.PUSH_PROVIDER

        subject.connect()

        verifySequence {
            fromIdleToConnectedSequence()
            configureSequence(false)
            mockLogger.i(capture(logSlot))
            mockVault.pushConfig
            mockStateChangedListener(fromConnectedToConfigured)
        }
        coVerify {
            mockPushService.synchronize(expectedDeviceToken, expectedPushProvider)
        }
        assertLogsForPushConfig()
    }

    private fun assertLogsForPushConfig() {
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CONNECT)
        assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.configureSession(Request.token))
        assertThat(logSlot[2].invoke()).isEqualTo(LogMessages.SYNCHRONIZE_PUSH_SERVICE_ON_SESSION_CONFIGURE)
    }
}
