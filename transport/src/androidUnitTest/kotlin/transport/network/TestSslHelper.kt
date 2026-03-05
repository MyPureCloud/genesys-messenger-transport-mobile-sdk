package transport.network

import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Loads the test PKCS12 keystore to create server and client SSL context
 * without using okhttp-tls. Used by HTTP/2 tests only.
 */
object TestSslHelper {

    private const val KEYSTORE_RESOURCE = "transport/network/test-keystore.p12"
    private const val STORE_PASSWORD = "changeit"
    private const val KEY_PASSWORD = "changeit"
    private const val ALIAS = "test"
    private const val PROTOCOL = "TLSv1.2"

    fun loadKeystore(): KeyStore {
        val stream = (
            Thread.currentThread().contextClassLoader?.getResourceAsStream(KEYSTORE_RESOURCE)
                ?: javaClass.classLoader?.getResourceAsStream(KEYSTORE_RESOURCE)
                ?: ClassLoader.getSystemResourceAsStream(KEYSTORE_RESOURCE)
            ) ?: error("Test keystore not found: $KEYSTORE_RESOURCE (tried context, class, and system classloaders)")
        return stream.use { KeyStore.getInstance("PKCS12").apply { load(it, STORE_PASSWORD.toCharArray()) } }
    }

    fun serverSslContext(): SSLContext {
        val keyStore = loadKeystore()
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, KEY_PASSWORD.toCharArray())
        val sslContext = SSLContext.getInstance(PROTOCOL)
        sslContext.init(kmf.keyManagers, null, null)
        return sslContext
    }

    fun clientSslContext(): SSLContext {
        val keyStore = loadKeystore()
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(keyStore)
        val sslContext = SSLContext.getInstance(PROTOCOL)
        sslContext.init(null, tmf.trustManagers, null)
        return sslContext
    }

    fun clientTrustManager(): X509TrustManager {
        val keyStore = loadKeystore()
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(keyStore)
        return tmf.trustManagers.filterIsInstance<X509TrustManager>().single()
    }
}
