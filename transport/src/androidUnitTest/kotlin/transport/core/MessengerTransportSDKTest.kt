package transport.core

import android.content.Context
import assertk.assertThat
import assertk.assertions.isInstanceOf
import com.genesys.cloud.messenger.transport.core.MessengerTransportSDK
import com.genesys.cloud.messenger.transport.util.DefaultVault
import com.genesys.cloud.messenger.transport.util.EncryptedVault
import com.genesys.cloud.messenger.transport.utility.FakeVault
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test

class MessengerTransportSDKTest {

    // Mocks
    private val mockContext = mockk<Context>(relaxed = true)
    private val mockApplicationContext = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        every { mockContext.applicationContext } returns mockApplicationContext

        DefaultVault.context = mockContext
        EncryptedVault.context = mockContext
    }

    @After
    fun tearDown() {
        DefaultVault.context = null
        EncryptedVault.context = null
    }

    @Test
    fun `should create DefaultVault when encryptedVault is false`() {
        val sdk = MessengerTransportSDK(TestValues.configuration)

        assertThat(sdk.vault).isInstanceOf(DefaultVault::class.java)
    }

    @Test
    fun `should create EncryptedVault when encryptedVault is true`() {
        val configuration = TestValues.configuration.copy(encryptedVault = true)

        val sdk = MessengerTransportSDK(configuration)

        assertThat(sdk.vault).isInstanceOf(EncryptedVault::class.java)
    }

    @Test
    fun `should use provided vault regardless of encryptedVault setting`() {
        val configuration = TestValues.configuration.copy(encryptedVault = true)
        val customVault = FakeVault(TestValues.vaultKeys)

        val sdk = MessengerTransportSDK(configuration, customVault)

        assertThat(sdk.vault).isInstanceOf(FakeVault::class.java)
    }
}
