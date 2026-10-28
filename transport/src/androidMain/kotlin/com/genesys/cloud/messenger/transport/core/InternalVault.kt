package com.genesys.cloud.messenger.transport.core

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.IOException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val vaultLock = Any()

internal fun <T> withVaultLock(operation: () -> T): T =
    synchronized(vaultLock, operation)

/**
 * Internal vault implementation for Android that uses Android KeyStore for encryption
 * and SharedPreferences for storing the encrypted data.
 *
 * @param serviceName The name of the service, used as a prefix for the SharedPreferences file
 */
internal class InternalVault(
    private val serviceName: String,
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SEPARATOR = "]"
        private const val KEY_ALIAS_PREFIX = "GenesysMessengerTransport_"
        private const val IV_LENGTH_BYTES = 12
        internal const val GCM_TAG_LENGTH_BITS = 128
        private const val GCM_TAG_LENGTH_BYTES = GCM_TAG_LENGTH_BITS / Byte.SIZE_BITS
    }

    private val keyAlias: String
        get() = "$KEY_ALIAS_PREFIX$serviceName"

    /**
     * Stores an encrypted string value in SharedPreferences using Android KeyStore for encryption.
     *
     * @param key The key to store
     * @param value The value to store
     * @return true if the value was stored, false if encryption failed
     */
    fun store(
        key: String,
        value: String
    ): Boolean {
        return withVaultLock {
            try {
                val encryptedData = encrypt(value)
                with(sharedPreferences.edit()) {
                    putString(key, encryptedData)
                    apply()
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Fetches and decrypts a string value from SharedPreferences.
     *
     * @param key The key to query
     * @return The decrypted string value, or null if it is missing or cannot be decrypted
     */
    fun fetch(key: String): String? {
        return withVaultLock {
            val encryptedData = sharedPreferences.getString(key, null)
            if (encryptedData == null) {
                null
            } else {
                try {
                    decrypt(encryptedData)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }
    }

    /**
     * Removes a key-value pair from SharedPreferences.
     *
     * @param key The key to remove
     */
    fun remove(key: String) {
        withVaultLock {
            with(sharedPreferences.edit()) {
                remove(key)
                apply()
            }
        }
    }

    /**
     * Re-stores in encrypted form any plaintext value found under the given keys,
     * such as values written by the 2.x DefaultVault or DefaultTokenStore.
     * Values that fail to encrypt are kept as they are so migration can retry on next launch.
     *
     * @param keys The keys to migrate
     */
    fun encryptPlaintextEntries(keys: Collection<String>) {
        withVaultLock {
            val entries = sharedPreferences.all
            keys.forEach { key ->
                val value = entries[key] as? String ?: return@forEach
                if (!isEncrypted(value)) store(key, value)
            }
        }
    }

    /**
     * Checks whether a stored value has the format produced by [encrypt], without decrypting it,
     * so values that can no longer be decrypted are never mistaken for plaintext.
     * The value must re-encode to itself because [Base64.decode] skips characters outside the alphabet.
     * Whitespace is ignored because SharedPreferences reads the stored value back with its XML indentation.
     */
    private fun isEncrypted(value: String): Boolean {
        if (!value.contains('\n')) return false
        val combined =
            try {
                Base64.decode(value, Base64.DEFAULT)
            } catch (e: IllegalArgumentException) {
                return false
            }
        return combined.size >= IV_LENGTH_BYTES + IV_SEPARATOR.length + GCM_TAG_LENGTH_BYTES &&
            combined[IV_LENGTH_BYTES] == IV_SEPARATOR[0].code.toByte() &&
            Base64.encodeToString(combined, Base64.NO_WRAP) == value.filterNot { it.isWhitespace() }
    }

    /**
     * Encrypts a string value using the Android KeyStore.
     *
     * @param plaintext The string to encrypt
     * @return Base64-encoded string containing the IV and encrypted data
     */
    @Throws(
        KeyStoreException::class,
        CertificateException::class,
        NoSuchAlgorithmException::class,
        IOException::class,
        UnrecoverableKeyException::class
    )
    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Combine IV and encrypted data with a separator
        val combined = iv + IV_SEPARATOR.toByteArray(Charsets.UTF_8) + encryptedBytes
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    /**
     * Decrypts a string value that was encrypted using the Android KeyStore.
     *
     * @param encryptedData Base64-encoded string containing the IV and encrypted data
     * @return The decrypted string
     */
    @Throws(
        KeyStoreException::class,
        CertificateException::class,
        NoSuchAlgorithmException::class,
        IOException::class,
        UnrecoverableKeyException::class
    )
    private fun decrypt(encryptedData: String): String {
        val combined = Base64.decode(encryptedData, Base64.DEFAULT)

        // Split at the fixed IV length, since the random IV itself can contain the separator byte
        if (combined.size <= IV_LENGTH_BYTES || combined[IV_LENGTH_BYTES] != IV_SEPARATOR[0].code.toByte()) {
            throw IllegalArgumentException("Invalid encrypted data format")
        }

        val iv = combined.sliceArray(0 until IV_LENGTH_BYTES)
        val encrypted = combined.sliceArray((IV_LENGTH_BYTES + 1) until combined.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

        val decryptedBytes = cipher.doFinal(encrypted)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Gets or creates a secret key from the Android KeyStore.
     *
     * @return The secret key for encryption/decryption
     */
    @Throws(
        KeyStoreException::class,
        CertificateException::class,
        NoSuchAlgorithmException::class,
        IOException::class
    )
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        return if (keyStore.containsAlias(keyAlias)) {
            try {
                getSecretKey()
            } catch (e: Exception) {
                generateSecretKey()
            }
        } else {
            generateSecretKey()
        }
    }

    /**
     * Gets an existing secret key from the Android KeyStore.
     *
     * @return The existing secret key
     */
    @Throws(
        KeyStoreException::class,
        CertificateException::class,
        NoSuchAlgorithmException::class,
        IOException::class,
        UnrecoverableKeyException::class
    )
    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        return keyStore.getKey(keyAlias, null) as SecretKey
    }

    /** Generates a new secret key in the Android KeyStore.
     *
     * @return The newly generated secret key
     */
    private fun generateSecretKey(): SecretKey {
        val keyGenParameterSpec =
            KeyGenParameterSpec
                .Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

        val keyGenerator =
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
}
