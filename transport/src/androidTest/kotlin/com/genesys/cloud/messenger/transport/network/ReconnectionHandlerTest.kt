package com.genesys.cloud.messenger.transport.network

import assertk.assertThat
import assertk.assertions.isFalse
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.mockk.MockKAnnotations
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class ReconnectionHandlerTest {
    internal val mockLogger: Log = mockk(relaxed = true)
    internal val logSlot = mutableListOf<() -> String>()
    private val mockReconnectFunction: () -> Unit = mockk(relaxed = true)
    private val dispatcher: CoroutineDispatcher = Dispatchers.Unconfined

    private var subject = ReconnectionHandlerImpl(TestValues.ReconnectionTimeout, mockLogger)

    @ExperimentalCoroutinesApi
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(dispatcher)
    }

    @ExperimentalCoroutinesApi
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

//    @Test
//    fun `when reconnect() and there are reconnection attempts left`(){
//        subject.reconnect { mockReconnectFunction  }
//
//        verify {
//            mockReconnectFunction.invoke()
//        }
//        assertThat(subject.shouldReconnect).isTrue()
//    }

    @Test
    fun `when reconnect() and there is NO reconnection attempts left`() {
        subject = ReconnectionHandlerImpl(TestValues.NoReconnectionAttempts, mockLogger)

        subject.reconnect { mockReconnectFunction }

        verify(exactly = 0) { mockReconnectFunction.invoke() }
        assertThat(subject.shouldReconnect).isFalse()
    }
}
