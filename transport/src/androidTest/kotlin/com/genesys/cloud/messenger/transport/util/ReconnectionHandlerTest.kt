package com.genesys.cloud.messenger.transport.util

import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

class ReconnectionHandlerTest {
    private val givenMaxReconnectionAttempts = 2
    private val testDispatcher = TestCoroutineDispatcher()
    private val mockReconnect: () -> Unit = mockk(relaxed = true)

    private val subject: ReconnectionHandler =
        ReconnectionHandlerImpl(givenMaxReconnectionAttempts, mockk(relaxed = true))

    @BeforeTest
    fun before() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun whenReconnect() {
        subject.reconnect(mockReconnect)

        assertTrue { subject.shouldReconnect() }
        coVerify {
            mockReconnect()
        }
    }

    @Test
    fun whenReconnectMoreThanAllowedAttempts() {
        for (i in 0..givenMaxReconnectionAttempts) {
            subject.reconnect(mockReconnect)
            testDispatcher.advanceTimeBy(i * DELAY_DELTA_IN_MILLISECONDS)
        }

        assertFalse { subject.shouldReconnect() }
        coVerify(exactly = givenMaxReconnectionAttempts) {
            mockReconnect()
        }
    }

    @Test
    fun whenExceedReconnectionAndThenResetAndReconnectAgain() {
        var index = 0
        while (subject.shouldReconnect()) {
            subject.reconnect(mockReconnect)
            testDispatcher.advanceTimeBy(index * DELAY_DELTA_IN_MILLISECONDS)
            index++
        }
        clearMocks(mockReconnect)

        subject.resetAttempts()

        assertTrue { subject.shouldReconnect() }
    }
}
