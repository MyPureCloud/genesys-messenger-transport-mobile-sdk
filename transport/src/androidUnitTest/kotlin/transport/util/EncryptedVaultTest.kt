package transport.util

import android.content.Context
import android.content.SharedPreferences
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.genesys.cloud.messenger.transport.core.InternalVault
import com.genesys.cloud.messenger.transport.util.EncryptedVault
import com.genesys.cloud.messenger.transport.util.Vault
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.mockk.*
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
    private val testKey = "test_key"
    private val testValue = "test_value"

    @Before
    fun setUp() {
        // Clear all mocks before each test
        clearAllMocks()
        
        // Set up context mocking
        every { mockContext.applicationContext } returns mockApplicationContext
        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockSharedPreferencesEditor
        every { mockSharedPreferencesEditor.apply() } just Runs
        
        // Set the context in the EncryptedVault companion object
        EncryptedVault.context = mockContext
        
        // Mock InternalVault constructor
        mockkConstructor(InternalVault::class)
        every { anyConstructed<InternalVault>().store(any(), any()) } just Runs
        every { anyConstructed<InternalVault>().remove(any()) } just Runs
    }

    @After
    fun tearDown() {
        // Clear the context reference after each test
        EncryptedVault.context = null
        unmockkAll()
    }


    @Test(expected = IllegalStateException::class)
    fun `test initialization throws exception when context is null`() {
        // Given a null context
        EncryptedVault.context = null
        
        // When creating an EncryptedVault instance, then it should throw IllegalStateException
        EncryptedVault(testKeys)
    }

    @Test
    fun `test store delegates to InternalVault`() {
        // Given an EncryptedVault instance
        val encryptedVault = EncryptedVault(testKeys)
        
        // When storing a key-value pair
        encryptedVault.store(testKey, testValue)
        
        // Then it should delegate to InternalVault
        verify { anyConstructed<InternalVault>().store(testKey, testValue) }
    }

    @Test
    fun `test fetch delegates to InternalVault`() {
        // Given an EncryptedVault instance and a mocked return value
        val encryptedVault = EncryptedVault(testKeys)
        every { anyConstructed<InternalVault>().fetch(testKey) } returns testValue
        
        // When fetching a value
        val result = encryptedVault.fetch(testKey)
        
        // Then it should delegate to InternalVault and return the expected value
        verify { anyConstructed<InternalVault>().fetch(testKey) }
        assertThat(result).isEqualTo(testValue)
    }

    @Test
    fun `test fetch returns null when key doesn't exist`() {
        // Given an EncryptedVault instance and a mocked null return value
        val encryptedVault = EncryptedVault(testKeys)
        every { anyConstructed<InternalVault>().fetch(testKey) } returns null
        
        // When fetching a non-existent key
        val result = encryptedVault.fetch(testKey)
        
        // Then it should return null
        assertThat(result).isNull()
    }

    @Test
    fun `test remove delegates to InternalVault`() {
        // Given an EncryptedVault instance
        val encryptedVault = EncryptedVault(testKeys)
        
        // When removing a key
        encryptedVault.remove(testKey)
        
        // Then it should delegate to InternalVault
        verify { anyConstructed<InternalVault>().remove(testKey) }
    }

    @Test
    fun `test context setter uses application context`() {
        // When setting the context
        EncryptedVault.context = mockContext
        
        // Then it should store a WeakReference to the application context
        verify { mockContext.applicationContext }
        
        // And the context getter should return the application context
        assertThat(EncryptedVault.context).isEqualTo(mockApplicationContext)
    }
}