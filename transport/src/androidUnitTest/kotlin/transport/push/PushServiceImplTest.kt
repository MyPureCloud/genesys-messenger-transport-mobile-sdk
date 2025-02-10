package transport.push

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.push.DEFAULT_PUSH_CONFIG
import com.genesys.cloud.messenger.transport.push.PushConfigComparator
import com.genesys.cloud.messenger.transport.push.PushConfigComparator.Diff
import com.genesys.cloud.messenger.transport.push.PushServiceImpl
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.util.Vault
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.utility.PushTestValues
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class PushServiceImplTest {

    private val mockVault: Vault = mockk {
        every { pushConfig } returns DEFAULT_PUSH_CONFIG
        every { token } returns TestValues.Token
    }
    private val mockPlatform: Platform = mockk {
        every { preferredLanguage() } returns TestValues.PREFERRED_LANGUAGE
        every { epochMillis() } returns TestValues.PUSH_SYNC_TIMESTAMP
        every { os } returns TestValues.DEVICE_TYPE
    }
    private val mockPushConfigComparator: PushConfigComparator = mockk {
        every { compare(any(), any()) } returns Diff.NONE
    }
    private val mockLogger: Log = mockk(relaxed = true)
    private val logSlot = mutableListOf<() -> String>()

    private val subject: PushServiceImpl =
        PushServiceImpl(mockVault, mockPlatform, mockPushConfigComparator, mockLogger)

    @Test
    fun `when synchronize`() {
        val expectedUserConfig = PushTestValues.CONFIG
        val expectedStoredConfig = DEFAULT_PUSH_CONFIG

        runBlocking {
            subject.synchronize(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)
        }

        verifySequence {
            mockLogger.i(capture(logSlot))
            mockVault.pushConfig
            mockVault.token
            mockPlatform.preferredLanguage()
            mockPlatform.epochMillis()
            mockPlatform.os
            mockPushConfigComparator.compare(expectedUserConfig, expectedStoredConfig)
        }
        assertThat(logSlot[0].invoke()).isEqualTo(
            LogMessages.synchronizingPush(TestValues.DEVICE_TOKEN, TestValues.PUSH_PROVIDER)
        )
    }
}
