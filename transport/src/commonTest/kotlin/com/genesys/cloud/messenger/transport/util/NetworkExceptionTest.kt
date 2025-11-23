package com.genesys.cloud.messenger.transport.util

import kotlin.test.Test
import kotlin.test.assertFalse

class NetworkExceptionTest {
    @Test
    fun `when exception is not a network exception`() {
        val exception = IllegalArgumentException("Some error")

        val result = exception.isNetworkException()

        assertFalse(result, "IllegalArgumentException should not be detected as network exception")
    }

    @Test
    fun `when exception with null message`() {
        val exception = Exception(null as String?)

        val result = exception.isNetworkException()

        assertFalse(result, "Exception with null message should not be detected as network exception")
    }

    @Test
    fun `when exception with empty message`() {
        val exception = Exception("")

        val result = exception.isNetworkException()

        assertFalse(result, "Exception with empty message should not be detected as network exception")
    }

    @Test
    fun `when exception with unrelated message`() {
        val exception = Exception("Something went wrong")

        val result = exception.isNetworkException()

        assertFalse(result, "Exception with unrelated message should not be detected as network exception")
    }
}
