package transport.util

import android.content.Context
import android.content.SharedPreferences
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.genesys.cloud.messenger.transport.core.InternalVault
import com.genesys.cloud.messenger.transport.util.EncryptedVault
import com.genesys.cloud.messenger.transport.util.VAULT_KEY
import com.genesys.cloud.messenger.transport.util.Vault
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

    // Test data
    private val testKeys = Vault.Keys(
        vaultKey = TestValues.VAULT_KEY,
        tokenKey = TestValues.TOKEN_KEY,
        authRefreshTokenKey = TestValues.AUTH_REFRESH_TOKEN_KEY,
        wasAuthenticated = TestValues.WAS_AUTHENTICATED
    )
    private val testKey = TestValues.VAULT_KEY
    private val testValue = TestValues.VAULT_VALUE
    @Before
    fun setUp() {
        clearAllMocks()

        every { mockContext.applicationContext } returns mockApplicationContext
        every { mockApplicationContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
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

        EncryptedVault(testKeys)
    }

    @Test
    fun `test store delegates to InternalVault`() {
        val encryptedVault = EncryptedVault(testKeys)

        encryptedVault.store(testKey, testValue)

        verify { anyConstructed<InternalVault>().store(testKey, testValue) }
    }

    @Test
    fun `test fetch delegates to InternalVault`() {
        val encryptedVault = EncryptedVault(testKeys)
        every { anyConstructed<InternalVault>().fetch(testKey) } returns testValue

        val result = encryptedVault.fetch(testKey)

        verify { anyConstructed<InternalVault>().fetch(testKey) }
        assertThat(result).isEqualTo(testValue)
    }

    @Test
    fun `test fetch returns null when key doesn't exist`() {
        val encryptedVault = EncryptedVault(testKeys)
        every { anyConstructed<InternalVault>().fetch(testKey) } returns null

        val result = encryptedVault.fetch(testKey)

        assertThat(result).isNull()
    }

    @Test
    fun `test remove delegates to InternalVault`() {
        val encryptedVault = EncryptedVault(testKeys)

        encryptedVault.remove(testKey)

        verify { anyConstructed<InternalVault>().remove(testKey) }
    }

    @Test
    fun `test context setter uses application context`() {
        EncryptedVault.context = mockContext

        verify { mockContext.applicationContext }
        assertThat(EncryptedVault.context).isEqualTo(mockApplicationContext)
    }

    @Test
    fun `test migration from default vault when data exists`() {
        val mockDefaultPrefs = mockk<SharedPreferences>(relaxed = true)
        val mockDefaultEditor = mockk<SharedPreferences.Editor>(relaxed = true)

        every { mockApplicationContext.getSharedPreferences(VAULT_KEY, any()) } returns mockDefaultPrefs
        every { mockApplicationContext.getSharedPreferences(testKeys.vaultKey, any()) } returns mockSharedPreferences
        every { mockDefaultPrefs.all } returns mapOf("key" to "value")
        every { mockDefaultPrefs.getString(testKeys.tokenKey, null) } returns "token_value"
        every { mockDefaultPrefs.getString(testKeys.authRefreshTokenKey, null) } returns "refresh_token"
        every { mockDefaultPrefs.getString(testKeys.wasAuthenticated, null) } returns "true"
        every { mockDefaultPrefs.edit() } returns mockDefaultEditor
        every { mockDefaultEditor.clear() } returns mockDefaultEditor
        every { mockDefaultEditor.apply() } just Runs

        EncryptedVault(testKeys)

        verify { anyConstructed<InternalVault>().store(testKeys.tokenKey, "token_value") }
        verify { anyConstructed<InternalVault>().store(testKeys.authRefreshTokenKey, "refresh_token") }
        verify { anyConstructed<InternalVault>().store(testKeys.wasAuthenticated, "true") }
        verify { mockDefaultEditor.clear() }
        verify { mockDefaultEditor.apply() }
    }

    @Test
    fun `test migration skips when default vault is empty`() {
        val mockDefaultPrefs = mockk<SharedPreferences>(relaxed = true)

        every { mockApplicationContext.getSharedPreferences(VAULT_KEY, any()) } returns mockDefaultPrefs
        every { mockApplicationContext.getSharedPreferences(testKeys.vaultKey, any()) } returns mockSharedPreferences
        every { mockDefaultPrefs.all } returns emptyMap()

        EncryptedVault(testKeys)

        verify(exactly = 0) { anyConstructed<InternalVault>().store(any(), any()) }
        verify(exactly = 0) { mockDefaultPrefs.edit() }
    }

    @Test
    fun `test migration handles null values gracefully`() {
        val mockDefaultPrefs = mockk<SharedPreferences>(relaxed = true)
        val mockDefaultEditor = mockk<SharedPreferences.Editor>(relaxed = true)

        every { mockApplicationContext.getSharedPreferences(VAULT_KEY, any()) } returns mockDefaultPrefs
        every { mockApplicationContext.getSharedPreferences(testKeys.vaultKey, any()) } returns mockSharedPreferences
        every { mockDefaultPrefs.all } returns mapOf("key" to "value")
        every { mockDefaultPrefs.getString(testKeys.tokenKey, null) } returns null
        every { mockDefaultPrefs.getString(testKeys.authRefreshTokenKey, null) } returns "refresh_token"
        every { mockDefaultPrefs.getString(testKeys.wasAuthenticated, null) } returns null
        every { mockDefaultPrefs.edit() } returns mockDefaultEditor
        every { mockDefaultEditor.clear() } returns mockDefaultEditor
        every { mockDefaultEditor.apply() } just Runs

        EncryptedVault(testKeys)

        verify(exactly = 0) { anyConstructed<InternalVault>().store(testKeys.tokenKey, any()) }
        verify { anyConstructed<InternalVault>().store(testKeys.authRefreshTokenKey, "refresh_token") }
        verify(exactly = 0) { anyConstructed<InternalVault>().store(testKeys.wasAuthenticated, any()) }
        verify { mockDefaultEditor.clear() }
        verify { mockDefaultEditor.apply() }
    }
}
