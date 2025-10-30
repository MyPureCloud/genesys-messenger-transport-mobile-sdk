package com.genesys.cloud.messenger.transport.core

import android.content.SharedPreferences
import android.os.Build
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
    }

    private val keyAlias: String
        get() = "$KEY_ALIAS_PREFIX$serviceName"

    /**
     * Stores an encrypted string value in SharedPreferences using Android KeyStore for encryption.
     *
     * @param key The key to store
     * @param value The value to store
     */
    fun store(
        key: String,
        value: String
    ) {
        try {
            val encryptedData = encrypt(value)
            with(sharedPreferences.edit()) {
                putString(key, encryptedData)
                apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Fetches and decrypts a string value from SharedPreferences.
     *
     * @param key The key to query
     * @return The decrypted string value, or null if it is missing or cannot be decrypted
     */
    fun fetch(key: String): String? {
        val encryptedData = sharedPreferences.getString(key, null) ?: return null
        return try {
            decrypt(encryptedData)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Removes a key-value pair from SharedPreferences.
     *
     * @param key The key to remove
     */
    fun remove(key: String) {
        with(sharedPreferences.edit()) {
            remove(key)
            apply()
        }
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

        // Split the combined array into IV and encrypted data
        val separatorIndex = combined.indexOf(IV_SEPARATOR[0].code.toByte())
        if (separatorIndex == -1) {
            throw IllegalArgumentException("Invalid encrypted data format")
        }

        val iv = combined.sliceArray(0 until separatorIndex)
        val encrypted = combined.sliceArray((separatorIndex + 1) until combined.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, iv)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
        } else {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            return keyGenerator.generateKey()
        }
    }
}
