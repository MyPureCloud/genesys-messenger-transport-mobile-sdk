package transport.core

import android.content.SharedPreferences
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
import io.mockk.mockkStatic
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

        subject.store(testKey, testValue)

        verifySequence {
            mockCipher.init(Cipher.ENCRYPT_MODE, mockSecretKey)
            mockCipher.iv
            mockCipher.doFinal(testValue.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(any(), Base64.DEFAULT)
            mockSharedPreferencesEditor.putString(testKey, testBase64)
            mockSharedPreferencesEditor.apply()
        }
    }

    @Test
    fun `test store handles encryption exceptions gracefully`() {
        val testMockKeyStore = mockk<KeyStore>(relaxed = true)
        every { KeyStore.getInstance(any()) } returns testMockKeyStore
        every { testMockKeyStore.load(null) } throws KeyStoreException("Test exception")

        subject.store(testKey, testValue)

        verify(exactly = 0) { mockSharedPreferencesEditor.putString(any(), any()) }
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
    fun `test cipher initialization failure during decryption returns null`() {
        every { mockSharedPreferences.getString(testKey, null) } returns testBase64
        every { Base64.decode(testBase64, Base64.DEFAULT) } returns givenTestCombined
        every { mockCipher.init(Cipher.DECRYPT_MODE, mockSecretKey, any<GCMParameterSpec>()) } throws
            Exception("Cipher initialization failed")

        val result = subject.fetch(testKey)

        assertThat(result).isNull()
    }
}
