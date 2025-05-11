package transport.core

import android.content.SharedPreferences
import android.util.Base64
import com.genesys.cloud.messenger.transport.core.InternalVault
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import java.security.KeyStoreException

class InternalVaultTest {

    // Mocks
    private val mockSharedPreferences = mockk<SharedPreferences>(relaxed = true)
    private val mockSharedPreferencesEditor = mockk<SharedPreferences.Editor>(relaxed = true)
    
    // Test data
    private val testServiceName = TestValues.VAULT_KEY
    private val testKey = "test_key"
    private val testValue = "test_value"
    
    // System under test
    private lateinit var internalVault: InternalVault
    
    @Before
    fun setUp() {
        // Clear all mocks before each test
        clearAllMocks()
        
        // Set up SharedPreferences mocking
        every { mockSharedPreferences.edit() } returns mockSharedPreferencesEditor
        every { mockSharedPreferencesEditor.putString(any(), any()) } returns mockSharedPreferencesEditor
        every { mockSharedPreferencesEditor.remove(any()) } returns mockSharedPreferencesEditor
        every { mockSharedPreferencesEditor.apply() } just Runs
        
        // Mock KeyStore and related classes
        mockkStatic(KeyStore::class)
        mockkStatic(KeyGenerator::class)
        mockkStatic(Cipher::class)
        mockkStatic(Base64::class)
        
        // Create the InternalVault instance
        internalVault = InternalVault(testServiceName, mockSharedPreferences)
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `test store saves encrypted value to SharedPreferences`() {
        // Mock the encryption process
        val mockCipher = mockk<Cipher>(relaxed = true)
        val mockSecretKey = mockk<SecretKey>(relaxed = true)
        val mockKeyStore = mockk<KeyStore>(relaxed = true)
        val testIv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        val testEncryptedBytes = byteArrayOf(20, 21, 22, 23, 24, 25)
        val testBase64 = "base64EncodedString"
        
        // Set up mocks for encryption
        every { KeyStore.getInstance(any()) } returns mockKeyStore
        every { mockKeyStore.load(null) } just Runs
        every { mockKeyStore.containsAlias(any()) } returns true
        every { mockKeyStore.getKey(any(), null) } returns mockSecretKey
        every { Cipher.getInstance(any()) } returns mockCipher
        every { mockCipher.init(Cipher.ENCRYPT_MODE, mockSecretKey) } just Runs
        every { mockCipher.iv } returns testIv
        every { mockCipher.doFinal(any<ByteArray>()) } returns testEncryptedBytes
        every { Base64.encodeToString(any(), Base64.DEFAULT) } returns testBase64
        
        // Execute the method under test
        internalVault.store(testKey, testValue)
        
        // Verify the encryption and storage process
        verify { mockCipher.init(Cipher.ENCRYPT_MODE, mockSecretKey) }
        verify { mockCipher.doFinal(testValue.toByteArray(Charsets.UTF_8)) }
        verify { Base64.encodeToString(any(), Base64.DEFAULT) }
        verify { mockSharedPreferencesEditor.putString(testKey, testBase64) }
        verify { mockSharedPreferencesEditor.apply() }
    }
    
    @Test
    fun `test store handles encryption exceptions gracefully`() {
        // Mock the encryption process to throw an exception
        val mockKeyStore = mockk<KeyStore>(relaxed = true)
        
        every { KeyStore.getInstance(any()) } returns mockKeyStore
        every { mockKeyStore.load(null) } throws KeyStoreException("Test exception")
        
        // Execute the method under test - should not throw an exception
        internalVault.store(testKey, testValue)
        
        // Verify that SharedPreferences was not updated
        verify(exactly = 0) { mockSharedPreferencesEditor.putString(any(), any()) }
    }
    
    @Test
    fun `test fetch returns decrypted value from SharedPreferences`() {
        // Mock the decryption process
        val mockCipher = mockk<Cipher>(relaxed = true)
        val mockSecretKey = mockk<SecretKey>(relaxed = true)
        val mockKeyStore = mockk<KeyStore>(relaxed = true)
        val testIv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        val testEncryptedBytes = byteArrayOf(20, 21, 22, 23, 24, 25)
        val testCombined = testIv + "]".toByteArray(Charsets.UTF_8) + testEncryptedBytes
        val testBase64 = "base64EncodedString"
        
        // Set up mocks for decryption
        every { mockSharedPreferences.getString(testKey, null) } returns testBase64
        every { KeyStore.getInstance(any()) } returns mockKeyStore
        every { mockKeyStore.load(null) } just Runs
        every { mockKeyStore.getKey(any(), null) } returns mockSecretKey
        every { Cipher.getInstance(any()) } returns mockCipher
        every { Base64.decode(testBase64, Base64.DEFAULT) } returns testCombined
        every { mockCipher.init(Cipher.DECRYPT_MODE, mockSecretKey, any<GCMParameterSpec>()) } just Runs
        every { mockCipher.doFinal(any<ByteArray>()) } returns testValue.toByteArray(Charsets.UTF_8)
        
        // Execute the method under test
        val result = internalVault.fetch(testKey)
        
        // Verify the decryption process
        verify { mockSharedPreferences.getString(testKey, null) }
        verify { Base64.decode(testBase64, Base64.DEFAULT) }
        verify { mockCipher.init(Cipher.DECRYPT_MODE, mockSecretKey, any<GCMParameterSpec>()) }
        verify { mockCipher.doFinal(any<ByteArray>()) }
        
        // Verify the result
        assertThat(result).isEqualTo(testValue)
    }
    
    @Test
    fun `test fetch returns null when key doesn't exist`() {
        // Set up mock to return null for the key
        every { mockSharedPreferences.getString(testKey, null) } returns null
        
        // Execute the method under test
        val result = internalVault.fetch(testKey)
        
        // Verify the result is null
        assertThat(result).isNull()
        
        // Verify SharedPreferences was queried
        verify { mockSharedPreferences.getString(testKey, null) }
    }
    
    @Test
    fun `test fetch returns null when decryption fails`() {
        // Mock the decryption process to throw an exception
        val testBase64 = "base64EncodedString"
        
        every { mockSharedPreferences.getString(testKey, null) } returns testBase64
        every { Base64.decode(testBase64, Base64.DEFAULT) } throws IllegalArgumentException("Invalid base64")
        
        // Execute the method under test
        val result = internalVault.fetch(testKey)
        
        // Verify the result is null
        assertThat(result).isNull()
    }
    
    @Test
    fun `test remove deletes key from SharedPreferences`() {
        // Execute the method under test
        internalVault.remove(testKey)
        
        // Verify SharedPreferences.Editor was used to remove the key
        verify { mockSharedPreferencesEditor.remove(testKey) }
        verify { mockSharedPreferencesEditor.apply() }
    }
    
    @Test
    fun `test fetch with invalid data format returns null`() {
        // Mock the decryption process with invalid data format (no separator)
        val testBase64 = "base64EncodedString"
        val invalidData = byteArrayOf(1, 2, 3, 4, 5) // No separator
        
        every { mockSharedPreferences.getString(testKey, null) } returns testBase64
        every { Base64.decode(testBase64, Base64.DEFAULT) } returns invalidData
        
        // Execute the method under test
        val result = internalVault.fetch(testKey)
        
        // Verify the result is null
        assertThat(result).isNull()
    }
    
    @Test
    fun `test store with empty value`() {
        // Mock the encryption process
        val mockCipher = mockk<Cipher>(relaxed = true)
        val mockSecretKey = mockk<SecretKey>(relaxed = true)
        val mockKeyStore = mockk<KeyStore>(relaxed = true)
        val testIv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        val testEncryptedBytes = byteArrayOf(20, 21, 22, 23, 24, 25)
        val testBase64 = "base64EncodedString"
        val emptyValue = ""
        
        // Set up mocks for encryption
        every { KeyStore.getInstance(any()) } returns mockKeyStore
        every { mockKeyStore.load(null) } just Runs
        every { mockKeyStore.containsAlias(any()) } returns true
        every { mockKeyStore.getKey(any(), null) } returns mockSecretKey
        every { Cipher.getInstance(any()) } returns mockCipher
        every { mockCipher.init(Cipher.ENCRYPT_MODE, mockSecretKey) } just Runs
        every { mockCipher.iv } returns testIv
        every { mockCipher.doFinal(any<ByteArray>()) } returns testEncryptedBytes
        every { Base64.encodeToString(any(), Base64.DEFAULT) } returns testBase64
        
        // Execute the method under test with empty string
        internalVault.store(testKey, emptyValue)
        
        // Verify the encryption and storage process still works with empty string
        verify { mockCipher.doFinal(emptyValue.toByteArray(Charsets.UTF_8)) }
        verify { mockSharedPreferencesEditor.putString(testKey, testBase64) }
    }
    
    @Test
    fun `test cipher initialization failure during decryption returns null`() {
        // Mock the decryption process with cipher initialization failure
        val mockCipher = mockk<Cipher>(relaxed = true)
        val mockSecretKey = mockk<SecretKey>(relaxed = true)
        val mockKeyStore = mockk<KeyStore>(relaxed = true)
        val testIv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        val testEncryptedBytes = byteArrayOf(20, 21, 22, 23, 24, 25)
        val testCombined = testIv + "]".toByteArray(Charsets.UTF_8) + testEncryptedBytes
        val testBase64 = "base64EncodedString"
        
        // Set up mocks for decryption with cipher initialization failure
        every { mockSharedPreferences.getString(testKey, null) } returns testBase64
        every { KeyStore.getInstance(any()) } returns mockKeyStore
        every { mockKeyStore.load(null) } just Runs
        every { mockKeyStore.getKey(any(), null) } returns mockSecretKey
        every { Cipher.getInstance(any()) } returns mockCipher
        every { Base64.decode(testBase64, Base64.DEFAULT) } returns testCombined
        every { mockCipher.init(Cipher.DECRYPT_MODE, mockSecretKey, any<GCMParameterSpec>()) } throws 
            Exception("Cipher initialization failed")
        
        // Execute the method under test
        val result = internalVault.fetch(testKey)
        
        // Verify the result is null
        assertThat(result).isNull()
    }
}