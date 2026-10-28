package transport.core

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.core.InternalVault
import com.genesys.cloud.messenger.transport.utility.DEFAULT_TIMEOUT
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.security.KeyStore
import java.security.KeyStoreException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class InternalVaultTest {
    @Test
    fun `when store() runs concurrently on instances sharing storage`() {
        val workerCount = 2
        val latchCount = 1
        val concurrentEntryObservationMillis = 500L
        val firstStoreEnteredKeyStore = CountDownLatch(latchCount)
        val releaseFirstStore = CountDownLatch(latchCount)
        val secondStoreStarted = CountDownLatch(latchCount)
        val secondStoreEnteredKeyStore = CountDownLatch(latchCount)
        val isFirstKeyStoreEntry = AtomicBoolean(true)
        val secondSubject = InternalVault(TestValues.SERVICE_NAME, mockSharedPreferences)

        every { mockCipher.iv } returns givenTestIv
        every { mockCipher.doFinal(any<ByteArray>()) } returns givenTestEncryptedBytes
        every { Base64.encodeToString(any(), Base64.DEFAULT) } returns testBase64
        every { mockKeyStore.containsAlias(any()) } answers {
            if (isFirstKeyStoreEntry.getAndSet(false)) {
                firstStoreEnteredKeyStore.countDown()
                releaseFirstStore.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
            } else {
                secondStoreEnteredKeyStore.countDown()
            }
            true
        }

        val executor = Executors.newFixedThreadPool(workerCount)
        val firstStoreFuture = executor.submit { subject.store(testKey, testValue) }
        var secondStoreFuture: Future<*>? = null
        var didFirstStoreEnterKeyStore = false
        var didSecondStoreStart = false
        var didSecondStoreEnterKeyStoreConcurrently = false

        try {
            didFirstStoreEnterKeyStore =
                firstStoreEnteredKeyStore.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
            if (didFirstStoreEnterKeyStore) {
                secondStoreFuture =
                    executor.submit {
                        secondStoreStarted.countDown()
                        secondSubject.store(testKey, testValue)
                    }
                didSecondStoreStart = secondStoreStarted.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                if (didSecondStoreStart) {
                    didSecondStoreEnterKeyStoreConcurrently =
                        secondStoreEnteredKeyStore.await(
                            concurrentEntryObservationMillis,
                            TimeUnit.MILLISECONDS
                        )
                }
            }
        } finally {
            releaseFirstStore.countDown()
            try {
                firstStoreFuture.get(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                secondStoreFuture?.get(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
            } finally {
                executor.shutdownNow()
                executor.awaitTermination(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
            }
        }

        assertThat(didFirstStoreEnterKeyStore).isTrue()
        assertThat(didSecondStoreStart).isTrue()
        assertThat(didSecondStoreEnterKeyStoreConcurrently).isFalse()
    }

    @Test
    fun `when fetch() and remove() run while store() accesses shared storage`() {
        val workerCount = 3
        val latchCount = 1
        val concurrentEntryObservationMillis = 500L
        val storeEnteredKeyStore = CountDownLatch(latchCount)
        val releaseStore = CountDownLatch(latchCount)
        val fetchStarted = CountDownLatch(latchCount)
        val removeStarted = CountDownLatch(latchCount)
        val fetchEnteredSharedPreferences = CountDownLatch(latchCount)
        val removeEnteredSharedPreferences = CountDownLatch(latchCount)
        val fetchSubject = InternalVault(TestValues.SERVICE_NAME, mockSharedPreferences)
        val removeSubject = InternalVault(TestValues.SERVICE_NAME, mockSharedPreferences)

        every { mockCipher.iv } returns givenTestIv
        every { mockCipher.doFinal(any<ByteArray>()) } returns givenTestEncryptedBytes
        every { Base64.encodeToString(any(), Base64.DEFAULT) } returns testBase64
        every { mockKeyStore.containsAlias(any()) } answers {
            storeEnteredKeyStore.countDown()
            releaseStore.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
            true
        }
        every { mockSharedPreferences.getString(testKey, null) } answers {
            fetchEnteredSharedPreferences.countDown()
            null
        }
        every { mockSharedPreferencesEditor.remove(testKey) } answers {
            removeEnteredSharedPreferences.countDown()
            mockSharedPreferencesEditor
        }

        val executor = Executors.newFixedThreadPool(workerCount)
        val storeFuture = executor.submit { subject.store(testKey, testValue) }
        var fetchFuture: Future<*>? = null
        var removeFuture: Future<*>? = null
        var didStoreEnterKeyStore = false
        var didFetchStart = false
        var didRemoveStart = false
        var didFetchEnterSharedPreferencesConcurrently = false
        var didRemoveEnterSharedPreferencesConcurrently = false

        try {
            didStoreEnterKeyStore = storeEnteredKeyStore.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
            if (didStoreEnterKeyStore) {
                fetchFuture =
                    executor.submit {
                        fetchStarted.countDown()
                        fetchSubject.fetch(testKey)
                    }
                removeFuture =
                    executor.submit {
                        removeStarted.countDown()
                        removeSubject.remove(testKey)
                    }
                didFetchStart = fetchStarted.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                didRemoveStart = removeStarted.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                if (didFetchStart) {
                    didFetchEnterSharedPreferencesConcurrently =
                        fetchEnteredSharedPreferences.await(
                            concurrentEntryObservationMillis,
                            TimeUnit.MILLISECONDS
                        )
                }
                if (didRemoveStart) {
                    didRemoveEnterSharedPreferencesConcurrently =
                        removeEnteredSharedPreferences.await(
                            concurrentEntryObservationMillis,
                            TimeUnit.MILLISECONDS
                        )
                }
            }
        } finally {
            releaseStore.countDown()
            try {
                storeFuture.get(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                fetchFuture?.get(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                removeFuture?.get(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
            } finally {
                executor.shutdownNow()
                executor.awaitTermination(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
            }
        }

        assertThat(didStoreEnterKeyStore).isTrue()
        assertThat(didFetchStart).isTrue()
        assertThat(didRemoveStart).isTrue()
        assertThat(didFetchEnterSharedPreferencesConcurrently).isFalse()
        assertThat(didRemoveEnterSharedPreferencesConcurrently).isFalse()
    }

    // Mocks
    private val mockSharedPreferences = mockk<SharedPreferences>(relaxed = true)
    private val mockSharedPreferencesEditor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val mockCipher = mockk<Cipher>(relaxed = true)
    private val mockSecretKey = mockk<SecretKey>(relaxed = true)
    private val mockKeyStore = mockk<KeyStore>(relaxed = true)

    private val testKey = TestValues.VAULT_KEY
    private val testValue = TestValues.VAULT_VALUE
    private val givenTestIv = TestValues.VAULT_IV
    private val givenTestEncryptedBytes = TestValues.VAULT_ENCRYPTED_BYTES
    private val testBase64 = TestValues.VAULT_BASE64
    private val givenTestCombined =
        givenTestIv + TestValues.VAULT_SEPARATOR.toByteArray(Charsets.UTF_8) + givenTestEncryptedBytes
    private val givenTagBytes = ByteArray(InternalVault.GCM_TAG_LENGTH_BITS / Byte.SIZE_BITS)
    private val givenEncryptedFormatBytes =
        givenTestIv + TestValues.VAULT_SEPARATOR.toByteArray(Charsets.UTF_8) + givenTagBytes
    private val givenStoredEncryptedValue = "$testBase64\n    "

    private lateinit var subject: InternalVault

    @Before
    fun setUp() {
        clearAllMocks()

        every { mockSharedPreferences.edit() } returns mockSharedPreferencesEditor
        every { mockSharedPreferencesEditor.putString(any(), any()) } returns mockSharedPreferencesEditor
        every { mockSharedPreferencesEditor.remove(any()) } returns mockSharedPreferencesEditor
        every { mockSharedPreferencesEditor.apply() } just Runs

        mockkStatic(KeyStore::class)
        mockkStatic(KeyGenerator::class)
        mockkStatic(Cipher::class)
        mockkStatic(Base64::class)

        every { KeyStore.getInstance(any()) } returns mockKeyStore
        every { mockKeyStore.containsAlias(any()) } returns true
        every { mockKeyStore.getKey(any(), null) } returns mockSecretKey
        every { Cipher.getInstance(any()) } returns mockCipher

        subject = InternalVault(TestValues.SERVICE_NAME, mockSharedPreferences)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test store saves encrypted value to SharedPreferences`() {
        every { mockCipher.iv } returns givenTestIv
        every { mockCipher.doFinal(any<ByteArray>()) } returns givenTestEncryptedBytes
        every { Base64.encodeToString(any(), Base64.DEFAULT) } returns testBase64

        val result = subject.store(testKey, testValue)

        verifySequence {
            mockCipher.init(Cipher.ENCRYPT_MODE, mockSecretKey)
            mockCipher.iv
            mockCipher.doFinal(testValue.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(any(), Base64.DEFAULT)
            mockSharedPreferencesEditor.putString(testKey, testBase64)
            mockSharedPreferencesEditor.apply()
        }
        assertThat(result).isTrue()
    }

    @Test
    fun `test store handles encryption exceptions gracefully`() {
        val testMockKeyStore = mockk<KeyStore>(relaxed = true)
        every { KeyStore.getInstance(any()) } returns testMockKeyStore
        every { testMockKeyStore.load(null) } throws KeyStoreException("Test exception")

        val result = subject.store(testKey, testValue)

        verify(exactly = 0) { mockSharedPreferencesEditor.putString(any(), any()) }
        assertThat(result).isFalse()
    }

    @Test
    fun `test fetch returns decrypted value from SharedPreferences`() {
        every { mockSharedPreferences.getString(testKey, null) } returns testBase64
        every { Base64.decode(testBase64, Base64.DEFAULT) } returns givenTestCombined
        every { mockCipher.doFinal(any<ByteArray>()) } returns testValue.toByteArray(Charsets.UTF_8)

        val result = subject.fetch(testKey)

        verifySequence {
            mockSharedPreferences.getString(testKey, null)
            Base64.decode(testBase64, Base64.DEFAULT)
            mockCipher.init(Cipher.DECRYPT_MODE, mockSecretKey, any<GCMParameterSpec>())
            mockCipher.doFinal(any<ByteArray>())
        }
        assertThat(result).isEqualTo(testValue)
    }

    @Test
    fun `test fetch returns null when key doesn't exist`() {
        every { mockSharedPreferences.getString(testKey, null) } returns null

        val result = subject.fetch(testKey)

        assertThat(result).isNull()
        verify { mockSharedPreferences.getString(testKey, null) }
    }

    @Test
    fun `test fetch returns null when decryption fails`() {
        every { mockSharedPreferences.getString(testKey, null) } returns testBase64
        every { Base64.decode(testBase64, Base64.DEFAULT) } throws IllegalArgumentException("Invalid base64")

        val result = subject.fetch(testKey)

        assertThat(result).isNull()
    }

    @Test
    fun `test remove deletes key from SharedPreferences`() {
        subject.remove(testKey)

        verify { mockSharedPreferencesEditor.remove(testKey) }
        verify { mockSharedPreferencesEditor.apply() }
    }

    @Test
    fun `test fetch with invalid data format returns null`() {
        val invalidData = byteArrayOf(1, 2, 3, 4, 5)
        every { mockSharedPreferences.getString(testKey, null) } returns testBase64
        every { Base64.decode(testBase64, Base64.DEFAULT) } returns invalidData

        val result = subject.fetch(testKey)

        assertThat(result).isNull()
    }

    @Test
    fun `test store with empty value`() {
        val emptyValue = ""
        every { mockCipher.iv } returns givenTestIv
        every { mockCipher.doFinal(any<ByteArray>()) } returns givenTestEncryptedBytes
        every { Base64.encodeToString(any(), Base64.DEFAULT) } returns testBase64

        subject.store(testKey, emptyValue)

        verify { mockCipher.doFinal(emptyValue.toByteArray(Charsets.UTF_8)) }
        verify { mockSharedPreferencesEditor.putString(testKey, testBase64) }
    }

    @Test
    fun `when store() and fetch() succeed with a newly generated key`() {
        val mockKeyGenerator = mockk<KeyGenerator>(relaxed = true)
        val mockKeyGenParameterSpec = mockk<KeyGenParameterSpec>()
        mockkConstructor(KeyGenParameterSpec.Builder::class)
        every { anyConstructed<KeyGenParameterSpec.Builder>().setBlockModes(any()) } answers { self as KeyGenParameterSpec.Builder }
        every {
            anyConstructed<KeyGenParameterSpec.Builder>().setEncryptionPaddings(any())
        } answers { self as KeyGenParameterSpec.Builder }
        every { anyConstructed<KeyGenParameterSpec.Builder>().setKeySize(any()) } answers { self as KeyGenParameterSpec.Builder }
        every { anyConstructed<KeyGenParameterSpec.Builder>().build() } returns mockKeyGenParameterSpec
        every { mockKeyStore.containsAlias(any()) } returns false
        every {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        } returns mockKeyGenerator
        every { mockKeyGenerator.generateKey() } returns mockSecretKey
        every { mockCipher.iv } returns givenTestIv
        every { mockCipher.doFinal(any<ByteArray>()) } returns givenTestEncryptedBytes
        every { Base64.encodeToString(any(), Base64.DEFAULT) } returns testBase64

        subject.store(testKey, testValue)

        verifySequence {
            mockKeyStore.load(null)
            mockKeyStore.containsAlias(any())
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            mockKeyGenerator.init(mockKeyGenParameterSpec)
            mockKeyGenerator.generateKey()
            mockCipher.init(Cipher.ENCRYPT_MODE, mockSecretKey)
            mockCipher.iv
            mockCipher.doFinal(testValue.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(any(), Base64.DEFAULT)
            mockSharedPreferencesEditor.putString(testKey, testBase64)
            mockSharedPreferencesEditor.apply()
        }

        every { mockSharedPreferences.getString(testKey, null) } returns testBase64
        every { Base64.decode(testBase64, Base64.DEFAULT) } returns givenTestCombined
        every { mockCipher.doFinal(any<ByteArray>()) } returns testValue.toByteArray(Charsets.UTF_8)

        val result = subject.fetch(testKey)

        assertThat(result).isEqualTo(testValue)
    }

    @Test
    fun `test cipher initialization failure during decryption returns null`() {
        every { mockSharedPreferences.getString(testKey, null) } returns testBase64
        every { Base64.decode(testBase64, Base64.DEFAULT) } returns givenTestCombined
        every { mockCipher.init(Cipher.DECRYPT_MODE, mockSecretKey, any<GCMParameterSpec>()) } throws
            Exception("Cipher initialization failed")

        val result = subject.fetch(testKey)

        assertThat(result).isNull()
    }

    @Test
    fun `when encryptPlaintextEntries() finds a plaintext value`() {
        every { mockSharedPreferences.all } returns mapOf(testKey to testValue)
        every { Base64.decode(testValue, Base64.DEFAULT) } throws IllegalArgumentException("bad base-64")
        every { mockCipher.iv } returns givenTestIv
        every { mockCipher.doFinal(any<ByteArray>()) } returns givenTestEncryptedBytes
        every { Base64.encodeToString(any(), Base64.DEFAULT) } returns testBase64

        subject.encryptPlaintextEntries(listOf(testKey))

        verify { mockCipher.doFinal(testValue.toByteArray(Charsets.UTF_8)) }
        verify { mockSharedPreferencesEditor.putString(testKey, testBase64) }
    }

    @Test
    fun `when encryptPlaintextEntries() finds an encrypted value`() {
        every { mockSharedPreferences.all } returns mapOf(testKey to givenStoredEncryptedValue)
        every { Base64.decode(givenStoredEncryptedValue, Base64.DEFAULT) } returns givenEncryptedFormatBytes
        every { Base64.encodeToString(givenEncryptedFormatBytes, Base64.NO_WRAP) } returns testBase64

        subject.encryptPlaintextEntries(listOf(testKey))

        verify(exactly = 0) { mockCipher.doFinal(any<ByteArray>()) }
        verify(exactly = 0) { mockSharedPreferencesEditor.putString(any(), any()) }
    }

    @Test
    fun `when encryptPlaintextEntries() runs again after migrating a plaintext value`() {
        every { mockSharedPreferences.all } returnsMany
            listOf(mapOf(testKey to testValue), mapOf(testKey to givenStoredEncryptedValue))
        every { Base64.decode(testValue, Base64.DEFAULT) } throws IllegalArgumentException("bad base-64")
        every { Base64.decode(givenStoredEncryptedValue, Base64.DEFAULT) } returns givenEncryptedFormatBytes
        every { Base64.encodeToString(givenEncryptedFormatBytes, Base64.NO_WRAP) } returns testBase64
        every { mockCipher.iv } returns givenTestIv
        every { mockCipher.doFinal(any<ByteArray>()) } returns givenTestEncryptedBytes
        every { Base64.encodeToString(any(), Base64.DEFAULT) } returns testBase64

        subject.encryptPlaintextEntries(listOf(testKey))
        subject.encryptPlaintextEntries(listOf(testKey))

        verify(exactly = 1) { mockSharedPreferencesEditor.putString(testKey, testBase64) }
    }

    @Test
    fun `when encryptPlaintextEntries() and store() fails because of KeyStoreException`() {
        every { mockSharedPreferences.all } returns mapOf(testKey to testValue)
        every { Base64.decode(testValue, Base64.DEFAULT) } throws IllegalArgumentException("bad base-64")
        every { mockKeyStore.load(null) } throws KeyStoreException("Test exception")

        subject.encryptPlaintextEntries(listOf(testKey))

        verify(exactly = 0) { mockSharedPreferencesEditor.putString(any(), any()) }
        verify(exactly = 0) { mockSharedPreferencesEditor.remove(any()) }
    }

    @Test
    fun `when encryptPlaintextEntries() finds a missing or non-String entry`() {
        val givenNonStringKey = TestValues.BOOLEAN_KEY
        every { mockSharedPreferences.all } returns mapOf(givenNonStringKey to true)

        subject.encryptPlaintextEntries(listOf(testKey, givenNonStringKey))

        verify(exactly = 0) { mockCipher.doFinal(any<ByteArray>()) }
        verify(exactly = 0) { mockSharedPreferencesEditor.putString(any(), any()) }
    }

    @Test
    fun `when encryptPlaintextEntries() finds Base64 values without the encrypted format`() {
        val givenTooShortKey = TestValues.TOKEN_KEY
        val givenTooShortValue = "tooShortValue\n"
        val givenMisplacedSeparatorKey = TestValues.AUTH_REFRESH_TOKEN_KEY
        val givenMisplacedSeparatorValue = "misplacedSeparatorValue\n"
        every { mockSharedPreferences.all } returns
            mapOf(
                givenTooShortKey to givenTooShortValue,
                givenMisplacedSeparatorKey to givenMisplacedSeparatorValue
            )
        every { Base64.decode(givenTooShortValue, Base64.DEFAULT) } returns givenTestCombined
        every { Base64.decode(givenMisplacedSeparatorValue, Base64.DEFAULT) } returns
            givenTestIv + givenTagBytes + TestValues.VAULT_SEPARATOR.toByteArray(Charsets.UTF_8)
        every { mockCipher.iv } returns givenTestIv
        every { mockCipher.doFinal(any<ByteArray>()) } returns givenTestEncryptedBytes
        every { Base64.encodeToString(any(), Base64.DEFAULT) } returns testBase64

        subject.encryptPlaintextEntries(listOf(givenTooShortKey, givenMisplacedSeparatorKey))

        verify { mockSharedPreferencesEditor.putString(givenTooShortKey, testBase64) }
        verify { mockSharedPreferencesEditor.putString(givenMisplacedSeparatorKey, testBase64) }
    }

    @Test
    fun `when encryptPlaintextEntries() finds a plaintext value that decodes to the encrypted format`() {
        val givenPlaintextValue = "{\"deviceToken\":\"lenient-decoded-value\"}\n"
        every { mockSharedPreferences.all } returns mapOf(testKey to givenPlaintextValue)
        every { Base64.decode(givenPlaintextValue, Base64.DEFAULT) } returns givenEncryptedFormatBytes
        every { Base64.encodeToString(givenEncryptedFormatBytes, Base64.NO_WRAP) } returns testBase64
        every { mockCipher.iv } returns givenTestIv
        every { mockCipher.doFinal(any<ByteArray>()) } returns givenTestEncryptedBytes
        every { Base64.encodeToString(any(), Base64.DEFAULT) } returns testBase64

        subject.encryptPlaintextEntries(listOf(testKey))

        verify { mockCipher.doFinal(givenPlaintextValue.toByteArray(Charsets.UTF_8)) }
        verify { mockSharedPreferencesEditor.putString(testKey, testBase64) }
    }

    @Test
    fun `when encryptPlaintextEntries() finds a plaintext value without a line break`() {
        every { mockSharedPreferences.all } returns mapOf(testKey to testBase64)
        every { mockCipher.iv } returns givenTestIv
        every { mockCipher.doFinal(any<ByteArray>()) } returns givenTestEncryptedBytes
        every { Base64.encodeToString(any(), Base64.DEFAULT) } returns testBase64

        subject.encryptPlaintextEntries(listOf(testKey))

        verify(exactly = 0) { Base64.decode(testBase64, Base64.DEFAULT) }
        verify { mockSharedPreferencesEditor.putString(testKey, testBase64) }
    }

    @Test
    fun `when fetch() decrypts a value whose IV contains the separator byte`() {
        val givenIvWithSeparator =
            givenTestIv.copyOf().also { it[0] = TestValues.VAULT_SEPARATOR[0].code.toByte() }
        val givenCombined =
            givenIvWithSeparator + TestValues.VAULT_SEPARATOR.toByteArray(Charsets.UTF_8) + givenTestEncryptedBytes
        val specSlot = slot<GCMParameterSpec>()
        every { mockSharedPreferences.getString(testKey, null) } returns testBase64
        every { Base64.decode(testBase64, Base64.DEFAULT) } returns givenCombined
        every { mockCipher.init(Cipher.DECRYPT_MODE, mockSecretKey, capture(specSlot)) } just Runs
        every { mockCipher.doFinal(givenTestEncryptedBytes) } returns testValue.toByteArray(Charsets.UTF_8)

        val result = subject.fetch(testKey)

        assertThat(result).isEqualTo(testValue)
        assertThat(specSlot.captured.iv.contentEquals(givenIvWithSeparator)).isTrue()
    }
}
