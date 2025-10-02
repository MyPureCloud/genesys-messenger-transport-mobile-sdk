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
        val subject = MessengerTransportSDK(TestValues.configuration)

        assertThat(subject.vault).isInstanceOf(DefaultVault::class.java)
    }

    @Test
    fun `should create EncryptedVault when encryptedVault is true`() {
        val configuration = TestValues.configuration.copy(encryptedVault = true)

        val subject = MessengerTransportSDK(configuration)

        assertThat(subject.vault).isInstanceOf(EncryptedVault::class.java)
    }

    @Test
    fun `should use provided vault regardless of encryptedVault setting`() {
        val configuration = TestValues.configuration.copy(encryptedVault = true)
        val fakeVault = FakeVault(TestValues.vaultKeys)

        val subject = MessengerTransportSDK(configuration, fakeVault)

        assertThat(subject.vault).isInstanceOf(FakeVault::class.java)
    }
}
