package com.genesys.cloud.messenger.transport.util

import org.junit.Test
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkExceptionAndroidTest {
    @Test
    fun `when exception is UnknownHostException`() {
        val exception = UnknownHostException("Unable to resolve host")

        val result = exception.isNetworkException()

        assertTrue(result, "UnknownHostException should be detected as network exception")
    }

    @Test
    fun `when exception is ConnectException`() {
        val exception = ConnectException("Connection refused")

        val result = exception.isNetworkException()

        assertTrue(result, "ConnectException should be detected as network exception")
    }

    @Test
    fun `when exception is SocketTimeoutException`() {
        val exception = SocketTimeoutException("Connection timed out")

        val result = exception.isNetworkException()

        assertTrue(result, "SocketTimeoutException should be detected as network exception")
    }

    @Test
    fun `when exception is SocketException`() {
        val exception = SocketException("Network is unreachable")

        val result = exception.isNetworkException()

        assertTrue(result, "SocketException should be detected as network exception")
    }

    @Test
    fun `when exception cause is UnknownHostException`() {
        val cause = UnknownHostException("Unable to resolve host")
        val exception = Exception("Wrapper exception", cause)

        val result = exception.isNetworkException()

        assertTrue(result, "Exception with UnknownHostException cause should be detected as network exception")
    }

    @Test
    fun `when exception cause is ConnectException`() {
        val cause = ConnectException("Connection refused")
        val exception = Exception("Wrapper exception", cause)

        val result = exception.isNetworkException()

        assertTrue(result, "Exception with ConnectException cause should be detected as network exception")
    }

    @Test
    fun `when exception cause is SocketTimeoutException`() {
        val cause = SocketTimeoutException("Connection timed out")
        val exception = Exception("Wrapper exception", cause)

        val result = exception.isNetworkException()

        assertTrue(result, "Exception with SocketTimeoutException cause should be detected as network exception")
    }

    @Test
    fun `when exception cause is SocketException`() {
        val cause = SocketException("Network is unreachable")
        val exception = Exception("Wrapper exception", cause)

        val result = exception.isNetworkException()

        assertTrue(result, "Exception with SocketException cause should be detected as network exception")
    }

    @Test
    fun `when exception is not a network exception and cause is also not`() {
        val cause = IllegalArgumentException("Invalid argument")
        val exception = Exception("Wrapper exception", cause)

        val result = exception.isNetworkException()

        assertFalse(result, "Exception with non-network cause should not be detected as network exception")
    }

    @Test
    fun `when exception is RuntimeException but not network related`() {
        val exception = RuntimeException("Some runtime error")

        val result = exception.isNetworkException()

        assertFalse(result, "RuntimeException without network cause should not be detected as network exception")
    }
}
