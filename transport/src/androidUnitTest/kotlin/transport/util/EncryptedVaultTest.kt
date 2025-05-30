package transport.util

import android.content.Context
import android.content.SharedPreferences
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.genesys.cloud.messenger.transport.core.InternalVault
import com.genesys.cloud.messenger.transport.util.EncryptedVault
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class EncryptedVaultTest {

    // Mocks
    private val mockContext = mockk<Context>(relaxed = true)
    private val mockApplicationContext = mockk<Context>(relaxed = true)
    private val mockSharedPreferences = mockk<SharedPreferences>(relaxed = true)
    private val mockSharedPreferencesEditor = mockk<SharedPreferences.Editor>(relaxed = true)

    private val testKey = TestValues.VAULT_KEY
    private val testValue = TestValues.VAULT_VALUE
    @Before
    fun setUp() {
        clearAllMocks()

        every { mockContext.applicationContext } returns mockApplicationContext
        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockSharedPreferencesEditor
        every { mockSharedPreferencesEditor.apply() } just Runs

        EncryptedVault.context = mockContext

        mockkConstructor(InternalVault::class)
        every { anyConstructed<InternalVault>().store(any(), any()) } just Runs
        every { anyConstructed<InternalVault>().remove(any()) } just Runs
    }

    @After
    fun tearDown() {
        EncryptedVault.context = null
        unmockkAll()
    }

    @Test(expected = IllegalStateException::class)
    fun `test initialization throws exception when context is null`() {
        EncryptedVault.context = null

        EncryptedVault(TestValues.vaultKeys)
    }

    @Test
    fun `test store delegates to InternalVault`() {
        val encryptedVault = EncryptedVault(TestValues.vaultKeys)

        encryptedVault.store(testKey, testValue)

        verify { anyConstructed<InternalVault>().store(testKey, testValue) }
    }

    @Test
    fun `test fetch delegates to InternalVault`() {
        val encryptedVault = EncryptedVault(TestValues.vaultKeys)
        every { anyConstructed<InternalVault>().fetch(testKey) } returns testValue

        val result = encryptedVault.fetch(testKey)

        verify { anyConstructed<InternalVault>().fetch(testKey) }
        assertThat(result).isEqualTo(testValue)
    }

    @Test
    fun `test fetch returns null when key doesn't exist`() {
        val encryptedVault = EncryptedVault(TestValues.vaultKeys)
        every { anyConstructed<InternalVault>().fetch(testKey) } returns null

        val result = encryptedVault.fetch(testKey)

        assertThat(result).isNull()
    }

    @Test
    fun `test remove delegates to InternalVault`() {
        val encryptedVault = EncryptedVault(TestValues.vaultKeys)

        encryptedVault.remove(testKey)

        verify { anyConstructed<InternalVault>().remove(testKey) }
    }

    @Test
    fun `test context setter uses application context`() {
        EncryptedVault.context = mockContext

        verify { mockContext.applicationContext }
        assertThat(EncryptedVault.context).isEqualTo(mockApplicationContext)
    }
}
